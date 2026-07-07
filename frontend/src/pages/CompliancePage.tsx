export function CompliancePage() {
  return (
    <section className="page-stack">
      <section className="panel" aria-labelledby="compliance-alerts-heading">
        <div className="section-heading">
          <div>
            <h2 id="compliance-alerts-heading">Compliance alerts</h2>
            <span>Consent, opt-out, guardian consent, and do-not-contact signals</span>
          </div>
        </div>
        <div
          className="alert-placeholder"
          role="status"
          aria-label="Compliance dashboard alert placeholder"
        >
          <strong>Alert feed placeholder</strong>
          <span>
            Upcoming alerts will surface pending approvals, consent exceptions, opt-outs,
            guardian-consent requirements, and do-not-contact overrides for Compliance Officer
            review.
          </span>
        </div>
      </section>

      <section className="panel" aria-labelledby="compliance-review-heading">
        <div className="section-heading">
          <h2 id="compliance-review-heading">Compliance review</h2>
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
    </section>
  );
}
