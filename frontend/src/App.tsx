import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import CrawlTargets from './pages/CrawlTargets'
import Articles from './pages/Articles'
import CrawlHistory from './pages/CrawlHistory'
import Report from './pages/Report'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="report" element={<Report />} />
          <Route path="targets" element={<CrawlTargets />} />
          <Route path="articles" element={<Articles />} />
          <Route path="history" element={<CrawlHistory />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
