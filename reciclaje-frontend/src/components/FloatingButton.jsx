// src/components/chatbot/FloatingButton.jsx
import React from "react";
import { useChatbot } from "./ChatbotContext";
import "../static/css/chatbot/FloatingButton.css";

const FloatingButton = () => {
  const { toggleChatbot, isChatbotVisible } = useChatbot();

  return (
    <button
      onClick={toggleChatbot}
      className="floating-button"
      aria-label={isChatbotVisible ? "Cerrar chat" : "Abrir chat"}
    >
      {isChatbotVisible ? "× Cerrar Chat" : "🌿 Chat Eco"}
    </button>
  );
};

export default FloatingButton;
