// ─── AI CHATBOT CONTROLLER (MEDICAL ASSISTANT) ───

const Chatbot = {
  messages: [
    {
      sender: 'ai',
      text: 'Hello! I am your NailVital AI Medical Assistant 🩺. Ask me anything about your nail scan results, symptoms, or dermatological care!'
    }
  ],

  async sendMessage(userMessage) {
    if (!userMessage || !userMessage.trim()) return null;

    const cleanMsg = userMessage.trim();
    this.messages.push({ sender: 'user', text: cleanMsg });

    try {
      const response = await ApiService.sendChatMessage(cleanMsg);
      const reply = response.reply || "I am here as your Medical Assistant to guide your nail health.";
      this.messages.push({ sender: 'ai', text: reply });
      return reply;
    } catch (err) {
      console.warn('[Chatbot] Backend chat error, using Medical Assistant response:', err.message);

      let fallbackText = "As your Medical Assistant, I can explain your nail health signals. ";
      const lower = cleanMsg.toLowerCase();

      if (lower.includes("line") || lower.includes("beau")) {
        fallbackText += "Horizontal ridges across nails (Beau's lines) are often caused by severe stress, high fever, or illness interrupting nail matrix growth.";
      } else if (lower.includes("spoon") || lower.includes("iron") || lower.includes("concave")) {
        fallbackText += "Spoon-shaped concave nails (Koilonychia) frequently point to iron deficiency. A routine blood ferritin test with a doctor is recommended.";
      } else if (lower.includes("fungus") || lower.includes("yellow")) {
        fallbackText += "Thickened or yellowish nails can indicate a fungal nail infection (Onychomycosis). Keeping your hands clean and completely dry is essential.";
      } else {
        fallbackText += "Be sure to track your daily scans and consult a dermatologist if you observe persistent discoloration or structural splitting.";
      }

      this.messages.push({ sender: 'ai', text: fallbackText });
      return fallbackText;
    }
  },

  clearHistory() {
    this.messages = [
      {
        sender: 'ai',
        text: 'Chat history cleared. How else can your Medical Assistant assist you today?'
      }
    ];
  }
};
