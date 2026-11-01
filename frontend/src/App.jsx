import { Route, Routes } from 'react-router-dom';

import Layout from './components/layout/Layout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';

import Home from './pages/Home.jsx';
import Services from './pages/Services.jsx';
import ServiceDetail from './pages/ServiceDetail.jsx';
import GetQuote from './pages/GetQuote.jsx';
import TrackBooking from './pages/TrackBooking.jsx';
import About from './pages/About.jsx';
import Contact from './pages/Contact.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Admin from './pages/Admin.jsx';
import NotFound from './pages/NotFound.jsx';

/**
 * The route table - a single readable map of the whole application.
 *
 * Structure worth noticing:
 *
 *   <Route element={<Layout />}>        a LAYOUT route: it has no path of its own, it only
 *                                       provides the shared navbar/footer shell via <Outlet/>
 *     <Route index element={<Home/>}/>  `index` means "the parent's path exactly", i.e. "/"
 *     <Route path="services/:slug"/>    a URL PARAMETER, read with useParams()
 *     <Route element={<ProtectedRoute/>}>   nested guard: every child inherits the check,
 *                                            so protection cannot be forgotten on a new page
 *     <Route path="*" element={<NotFound/>} />  the catch-all, always last
 *
 * Nesting the guard rather than repeating it inside each page is the key idea: add a new admin
 * screen under that branch and it is protected by construction.
 */
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        {/* ---------- public ---------- */}
        <Route index element={<Home />} />
        <Route path="services" element={<Services />} />
        <Route path="services/:slug" element={<ServiceDetail />} />
        <Route path="quote" element={<GetQuote />} />
        <Route path="track" element={<TrackBooking />} />
        <Route path="about" element={<About />} />
        <Route path="contact" element={<Contact />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />

        {/* ---------- signed-in customers ---------- */}
        <Route element={<ProtectedRoute />}>
          <Route path="dashboard" element={<Dashboard />} />
        </Route>

        {/* ---------- admins only ---------- */}
        <Route element={<ProtectedRoute adminOnly />}>
          <Route path="admin" element={<Admin />} />
        </Route>

        {/* Must be last: "*" matches anything that reached this point unmatched. */}
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
