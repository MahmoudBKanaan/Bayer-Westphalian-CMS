export function CompliancePage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Compliance review</h2>
        <span>Campaign approval, consent status, exclusions, and audit evidence</span>
      </div>
      <div className="work-list">
        <div>
          <strong>Grandchild Education Plan</strong>
          <span>Submitted for Compliance Officer approval</span>
        </div>
        <div>
          <strong>Consent eligibility check</strong>
          <span>258 recipients excluded before launch</span>
        </div>
        <div>
          <strong>Guardian consent</strong>
          <span>Minor beneficiaries require valid guardian consent</span>
        </div>
      </div>
    </section>
  );
}
