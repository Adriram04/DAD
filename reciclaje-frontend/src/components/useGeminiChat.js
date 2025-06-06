// src/components/chatbot/useGeminiChat.js
import { useState } from "react";
import { useChatbot } from "./ChatbotContext";

const API_CHAT_ENDPOINT = "https://api.ecobins.tech/api/chat"; 
// Asegúrate de que tu backend tenga un endpoint POST /api/chat
// que reciba { message: string, chatHistory: [...] } y devuelva { reply: string }.

export default function useGeminiChat() {
  const [loading, setLoading] = useState(false);
  const { chatHistory, setChatHistory } = useChatbot();

  const sendMessage = async (message) => {
    setLoading(true);
    try {
      // Preparamos el body con el mensaje nuevo y el historial actual
      const payload = {
        message,
        history: chatHistory, 
      };

      const res = await fetch(API_CHAT_ENDPOINT, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }

      const data = await res.json();
      // Esperamos que la respuesta tenga { reply: "Texto de respuesta" }
      if (data.reply) {
        // Agregar al historial la respuesta del bot
        setChatHistory(prev => [...prev, { sender: "Bot", text: data.reply }]);
      } else {
        throw new Error("No se recibió respuesta del chat.");
      }
    } finally {
      setLoading(false);
    }
  };

  return { sendMessage, loading };
}
