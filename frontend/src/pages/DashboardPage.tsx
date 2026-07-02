import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { MetricCard } from "@/components/MetricCard";
import { StatusBadge } from "@/components/StatusBadge";
import { campaigns, performance } from "@/features/dashboard/mockData";
import { formatNumber, formatPercent } from "@/utils/format";

export function DashboardPage() {
  const totalAudience = campaigns.reduce((sum, campaign) => sum + campaign.audience, 0);
  const totalEligible = campaigns.reduce((sum, campaign) => sum + campaign.eligible, 0);
  const eligibilityRate = (totalEligible / totalAudience) * 100;

  return (
    <section className="page-stack">
      <div className="metric-grid">
        <MetricCard label="Campaigns" value="3" detail="Draft, submitted, and approved" />
        <MetricCard
          label="Audience"
          value={formatNumber(totalAudience)}
          detail="Total previewed contacts"
        />
        <MetricCard
          label="Eligible"
          value={formatPercent(eligibilityRate)}
          detail="After consent and opt-out checks"
        />
        <MetricCard label="Pending approvals" value="1" detail="Compliance review required" />
      </div>

      <section className="panel">
        <div className="section-heading">
          <h2>Campaign performance</h2>
          <span>Sent messages vs conversions</span>
        </div>
        <div className="chart-frame">
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={performance}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="sent" fill="#2563eb" radius={[4, 4, 0, 0]} />
              <Bar dataKey="conversions" fill="#16a34a" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>

      <section className="panel">
        <div className="section-heading">
          <h2>Active campaign work</h2>
          <span>Consent-aware campaign lifecycle</span>
        </div>
        <table>
          <thead>
            <tr>
              <th>Campaign</th>
              <th>Status</th>
              <th>Audience</th>
              <th>Eligible</th>
              <th>Excluded</th>
            </tr>
          </thead>
          <tbody>
            {campaigns.map((campaign) => (
              <tr key={campaign.id}>
                <td>{campaign.name}</td>
                <td>
                  <StatusBadge value={campaign.status} />
                </td>
                <td>{formatNumber(campaign.audience)}</td>
                <td>{formatNumber(campaign.eligible)}</td>
                <td>{formatNumber(campaign.excluded)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </section>
  );
}
