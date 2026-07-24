import type { AccountResponse } from '../api/types'

/** Dropdown of accounts, labelled "Name (TYPE · CUR)". */
export default function AccountSelect({
  accounts,
  value,
  onChange,
  placeholder = 'Select account…',
}: {
  accounts: AccountResponse[]
  value: string
  onChange: (id: string) => void
  placeholder?: string
}) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">{placeholder}</option>
      {accounts.map((a) => (
        <option key={a.id} value={a.id}>
          {a.name} ({a.type} · {a.currency})
        </option>
      ))}
    </select>
  )
}
