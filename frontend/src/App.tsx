import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Landing from './pages/Landing'
import Dashboard from './pages/Dashboard'
import CrawlTargets from './pages/CrawlTargets'
import Articles from './pages/Articles'
import CrawlHistory from './pages/CrawlHistory'
import Report from './pages/Report'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Landing Page */}
        <Route path="/" element={<Landing />} />

        {/* App Routes with Layout */}
        <Route path="/dashboard" element={<Layout />}>
          <Route index element={<Dashboard />} />
        </Route>
        <Route path="/report" element={<Layout />}>
          <Route index element={<Report />} />
        </Route>
        <Route path="/targets" element={<Layout />}>
          <Route index element={<CrawlTargets />} />
        </Route>
        <Route path="/articles" element={<Layout />}>
          <Route index element={<Articles />} />
        </Route>
        <Route path="/history" element={<Layout />}>
          <Route index element={<CrawlHistory />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
