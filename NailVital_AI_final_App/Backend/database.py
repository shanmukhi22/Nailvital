import os
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv

env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")
load_dotenv(dotenv_path=env_path)

# Defaulting to XAMPP MySQL if missing from .env
SQLALCHEMY_DATABASE_URL = os.getenv("DATABASE_URL", "mysql+mysqlconnector://root:@127.0.0.1/nailvital")

# Configure engine for MySQL
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, 
    pool_pre_ping=True, 
    pool_recycle=3600
)

DB_CONNECTED = False 
DB_CHECK_DONE = False

def check_db_connection():
    global DB_CONNECTED, DB_CHECK_DONE
    if DB_CHECK_DONE and DB_CONNECTED:
        return DB_CONNECTED
    try:
        from sqlalchemy.sql import text
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        DB_CONNECTED = True
        DB_CHECK_DONE = True
        print("[DB] MySQL Database connection verified.")
    except Exception as e:
        print(f"WARNING: MySQL Database unreachable: {e}")
        DB_CONNECTED = False
        DB_CHECK_DONE = False
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
