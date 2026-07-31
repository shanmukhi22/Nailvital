import os
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv

env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")
load_dotenv(dotenv_path=env_path)

# Determine Database Connection URL with resilient SQLite fallback
raw_db_url = os.getenv("DATABASE_URL", "sqlite:///./sql_app.db")
if raw_db_url.startswith("postgres://"):
    raw_db_url = raw_db_url.replace("postgres://", "postgresql://", 1)

SQLALCHEMY_DATABASE_URL = raw_db_url
engine_args = {}

if "postgresql" in raw_db_url:
    from sqlalchemy.sql import text
    try:
        test_engine = create_engine(raw_db_url, connect_args={"connect_timeout": 3})
        with test_engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        print("[DB] Connected to PostgreSQL / Supabase.")
        engine_args = {
            "pool_pre_ping": True, 
            "pool_recycle": 3600,
            "connect_args": {"connect_timeout": 5}
        }
    except Exception as e:
        print(f"[DB WARN] Cloud Supabase connection failed ({e}). Falling back to local SQLite database.")
        SQLALCHEMY_DATABASE_URL = "sqlite:///./sql_app.db"
        engine_args = {"connect_args": {"check_same_thread": False}}
elif "sqlite" in raw_db_url:
    engine_args = {"connect_args": {"check_same_thread": False}}

engine = create_engine(SQLALCHEMY_DATABASE_URL, **engine_args)

# DB_CONNECTED will be set to True only if a connection succeeds later.
# We skip the blocking check at startup to prevent uvicorn reload hangs.
DB_CONNECTED = False 
DB_CHECK_DONE = False

def check_db_connection():
    global DB_CONNECTED, DB_CHECK_DONE
    # Only cache successes - always retry if previously failed
    if DB_CHECK_DONE and DB_CONNECTED:
        return DB_CONNECTED
    try:
        from sqlalchemy.sql import text
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        DB_CONNECTED = True
        DB_CHECK_DONE = True
        print("Database connection verified.")
    except Exception:
        print("WARNING: Supabase unreachable. Skipping persistent storage.")
        DB_CONNECTED = False
        DB_CHECK_DONE = False  # Keep retrying on next request
    return DB_CONNECTED

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

# Dependency to get DB session
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
