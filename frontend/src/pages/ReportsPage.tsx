export function ReportsPage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Reports</h2>
        <span>CSV and PDF exports for campaigns, compliance, and executive review</span>
      </div>
      <div className="button-row">
        <button type="button">Campaign CSV</button>
        <button type="button">Campaign PDF</button>
        <button type="button">Compliance report</button>
      </div>
    </section>
  );
}
