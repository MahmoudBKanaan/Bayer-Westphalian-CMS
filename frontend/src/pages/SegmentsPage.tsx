export function SegmentsPage() {
  return (
    <section className="page-stack">
      <section className="panel">
        <div className="section-heading">
          <h2>Segmentation</h2>
          <span>Reusable audience criteria with eligibility-aware preview</span>
        </div>
        <div className="split-grid">
          <div className="criteria-box">Age group: 18-35</div>
          <div className="criteria-box">Product ownership: No homeowner insurance</div>
          <div className="criteria-box">Consent: Valid marketing consent</div>
          <div className="criteria-box">Exclusions: Opt-out, do-not-contact, monthly limit</div>
        </div>
      </section>
    </section>
  );
}
