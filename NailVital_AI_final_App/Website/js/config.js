// ─── CONFIGURATION & LOCAL DEMO FALLBACK DATABASE ───

const CONFIG = {
  API_BASE_URL: 'http://10.14.242.73:8000',
  TOKEN_KEY: 'nailvital_access_token',
  USER_KEY: 'nailvital_user_info',

  DISEASE_DETAILS: {
    "aloperia_areata": {
      name: "Alopecia Areata (Nail Changes)",
      description: "Small pits, horizontal ridges, or rough texture. Autoimmune matrix response.",
      recommendation: "Consult a dermatologist for autoimmune evaluation and topical care."
    },
    "beaus_lines": {
      name: "Beau's Lines",
      description: "Deep grooved lines running horizontally across the nail caused by temporary growth arrest.",
      recommendation: "Ensure proper nutrition and hydrate as the nail grows out cleanly."
    },
    "bluish_nail": {
      name: "Bluish Nails (Cyanosis)",
      description: "Bluish tint indicating lower oxygen saturation in circulating blood.",
      recommendation: "Monitor pulse oximetry. Seek medical check if persistent."
    },
    "clubbing": {
      name: "Nail Clubbing",
      description: "Bulbous enlargement of fingertips with downward nail curvature.",
      recommendation: "Urgent cardiovascular & pulmonary evaluation recommended."
    },
    "koilonychia": {
      name: "Koilonychia (Spoon Nails)",
      description: "Soft, concave nails shaped like spoons, frequently linked to iron deficiency.",
      recommendation: "Schedule a complete blood count test and increase dietary iron."
    },
    "leukonychia": {
      name: "Leukonychia (White Spots)",
      description: "Harmless white micro-punctate marks caused by minor matrix micro-trauma.",
      recommendation: "No treatment required. Maintain balanced mineral intake (Zinc/Calcium)."
    },
    "onychomycosis": {
      name: "Onychomycosis (Nail Fungus)",
      description: "Fungal infection resulting in thickened, yellowish, brittle nail texture.",
      recommendation: "Keep nails dry and clean. Consider topical or oral antifungal solutions."
    },
    "healthy": {
      name: "Healthy Nails",
      description: "Smooth, uniform color and surface contour. No pathological signals detected.",
      recommendation: "Maintain good hydration, cuticle care, and daily nutrition."
    }
  }
};
