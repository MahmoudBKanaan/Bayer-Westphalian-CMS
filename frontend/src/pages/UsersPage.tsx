export function UsersPage() {
  const roles = [
    "Admin",
    "Campaign Manager",
    "BI Analyst",
    "Product Manager",
    "Compliance Officer",
    "Customer Service Agent",
  ];

  return (
    <section className="panel">
      <div className="section-heading">
        <h2>User management</h2>
        <span>Employee accounts, role assignment, and protected routes</span>
      </div>
      <div className="role-grid">
        {roles.map((role) => (
          <div className="criteria-box" key={role}>
            {role}
          </div>
        ))}
      </div>
    </section>
  );
}
