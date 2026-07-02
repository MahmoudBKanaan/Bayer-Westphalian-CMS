export function AuditPage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Audit log</h2>
        <span>Sensitive actions, consent changes, approvals, and role updates</span>
      </div>
      <div className="work-list">
        <div>
          <strong>Consent updated</strong>
          <span>Customer Service Agent changed consent status</span>
        </div>
        <div>
          <strong>Campaign approved</strong>
          <span>Compliance Officer approved submitted campaign</span>
        </div>
        <div>
          <strong>Role changed</strong>
          <span>Admin assigned Campaign Manager role</span>
        </div>
      </div>
    </section>
  );
}
