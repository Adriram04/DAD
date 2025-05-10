import React, { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import Login from '../auth/login/Login'
import "../static/css/auth/Landing.css"

export default function Landing() {
  const [showLogo, setShowLogo] = useState(true)
  const [fadeLogo, setFadeLogo] = useState(false)

  useEffect(() => {
    // 5s en estado visible, luego fade de 1s, y al final ocultar
    const t1 = setTimeout(() => setFadeLogo(true), 5000)
    const t2 = setTimeout(() => setShowLogo(false), 6000)
    return () => {
      clearTimeout(t1)
      clearTimeout(t2)
    }
  }, [])

  return (
    <div className="landing-container">
      {/** 1) Logo fullscreen palpitando **/}
      {showLogo && (
        <div className={`logo-overlay ${fadeLogo ? 'fade-out' : ''}`}>
          <img src="/logo.png" alt="EcoTech" className="logo-img pulse-soft" />
        </div>
      )}

      {/** 2) Login con fondo degradado + logo difuminado **/}
      {!showLogo && (
        <>
          <div className="final-bg" />
          <div className="login-wrapper">
            <Login />
          </div>
        </>
      )}
    </div>
  )
}
