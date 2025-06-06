// src/components/chatbot/ChatbotContext.jsx
import React, { createContext, useState, useContext } from "react";

const ChatbotContext = createContext(null);

export const useChatbot = () => {
  return useContext(ChatbotContext);
};

export const ChatbotProvider = ({ children }) => {
  const [isChatbotVisible, setChatbotVisible] = useState(false);
  const [chatHistory, setChatHistory] = useState([]);

  const toggleChatbot = () => {
    setChatbotVisible(prev => !prev);
  };

  return (
    <ChatbotContext.Provider
      value={{ isChatbotVisible, toggleChatbot, chatHistory, setChatHistory }}
    >
      {children}
    </ChatbotContext.Provider>
  );
};
