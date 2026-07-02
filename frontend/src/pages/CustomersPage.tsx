import { StatusBadge } from "@/components/StatusBadge";
import { customers } from "@/features/dashboard/mockData";

export function CustomersPage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Customers and prospects</h2>
        <span>Profile, consent status, products, beneficiaries, and contact history</span>
      </div>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Segment</th>
            <th>City</th>
            <th>Products</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {customers.map((customer) => (
            <tr key={customer.id}>
              <td>{customer.id}</td>
              <td>{customer.name}</td>
              <td>{customer.segment}</td>
              <td>{customer.city}</td>
              <td>{customer.ownedProducts}</td>
              <td>
                <StatusBadge value={customer.status} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
