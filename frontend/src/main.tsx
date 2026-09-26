import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { initI18n } from './app/i18n'
import { printConsoleGreeting } from './shared/easter/consoleGreeting'
import App from './app/App.tsx'

initI18n()
printConsoleGreeting(navigator.language)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
