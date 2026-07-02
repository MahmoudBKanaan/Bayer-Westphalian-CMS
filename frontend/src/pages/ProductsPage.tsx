import { products } from "@/features/dashboard/mockData";
import { formatNumber } from "@/utils/format";

export function ProductsPage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Products</h2>
        <span>Insurance and investment products used by campaigns and reminders</span>
      </div>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Product</th>
            <th>Category</th>
            <th>Active policies</th>
            <th>Expiration campaigns</th>
          </tr>
        </thead>
        <tbody>
          {products.map((product) => (
            <tr key={product.id}>
              <td>{product.id}</td>
              <td>{product.name}</td>
              <td>{product.category}</td>
              <td>{formatNumber(product.activePolicies)}</td>
              <td>{product.expirationCampaigns}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
