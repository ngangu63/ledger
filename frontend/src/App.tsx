import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import RequireAuth from './components/RequireAuth'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Accounts from './pages/Accounts'
import AccountDetail from './pages/AccountDetail'
import Transactions from './pages/Transactions'
import TransactionDetail from './pages/TransactionDetail'
import Cash from './pages/Cash'
import Payments from './pages/Payments'
import PaymentDetail from './pages/PaymentDetail'
import BankTransfers from './pages/BankTransfers'
import BankTransferDetail from './pages/BankTransferDetail'
import Reimbursements from './pages/Reimbursements'
import ReimbursementDetail from './pages/ReimbursementDetail'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          element={
            <RequireAuth>
              <Layout />
            </RequireAuth>
          }
        >
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/accounts" element={<Accounts />} />
          <Route path="/accounts/:id" element={<AccountDetail />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/transactions/:id" element={<TransactionDetail />} />
          <Route path="/cash" element={<Cash />} />
          <Route path="/payments" element={<Payments />} />
          <Route path="/payments/:id" element={<PaymentDetail />} />
          <Route path="/bank-transfers" element={<BankTransfers />} />
          <Route path="/bank-transfers/:id" element={<BankTransferDetail />} />
          <Route path="/reimbursements" element={<Reimbursements />} />
          <Route path="/reimbursements/:id" element={<ReimbursementDetail />} />
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
