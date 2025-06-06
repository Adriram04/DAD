// src/components/chatbot/EcoChatbot.jsx
import React, { useState } from "react";
import { useChatbot } from "./ChatbotContext";
import useEcoChat from "./useGeminiChat";
import "../static/css/chatbot/Chatbot.css";

const EcoChatbot = () => {
  const [message, setMessage] = useState("");
  const { sendMessage, loading } = useEcoChat();
  const { chatHistory, setChatHistory } = useChatbot();
  const [error, setError] = useState("");

  const handleSend = async () => {
    if (!message.trim()) return;
    try {
      setChatHistory(prev => [...prev, { sender: "Usuario", text: message }]);
      await sendMessage(message);
      setMessage("");
      setError("");
    } catch (err) {
      console.error("Error al enviar mensaje:", err);
      setError("Error enviando el mensaje. Intenta nuevamente.");
    }
  };

  return (
    <div className="chatbot-container">
      <div className="chatbot-header">
        <h3>Chat Eco</h3>
      </div>

      <div className="chatbot-body">
        {chatHistory.map((chat, index) => (
          <div
            key={index}
            className={`chat-message ${chat.sender === "Bot" ? "bot" : "user"}`}
          >
            <strong>{chat.sender}:</strong>
            <p>{chat.text}</p>
          </div>
        ))}
        {error && (
          <div className="chat-error">
            <strong>Error:</strong>
            <p>{error}</p>
          </div>
        )}
      </div>

      <div className="chatbot-footer">
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Escribe tu duda sobre contenedores o reciclaje..."
          aria-label="Escribe tu duda sobre contenedores o reciclaje"
          rows={2}
        />
        <button onClick={handleSend} disabled={loading || !message.trim()}>
          {loading ? "Pensando…" : "Enviar"}
        </button>
      </div>
    </div>
  );
};

export default EcoChatbot;
