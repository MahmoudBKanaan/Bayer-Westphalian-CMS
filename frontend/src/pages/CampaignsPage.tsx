import { StatusBadge } from "@/components/StatusBadge";
import { campaigns } from "@/features/dashboard/mockData";
import { formatNumber } from "@/utils/format";

export function CampaignsPage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Campaigns</h2>
        <span>Draft, submit, approve, launch, pause, complete, and archive</span>
      </div>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Owner</th>
            <th>Status</th>
            <th>Eligible recipients</th>
          </tr>
        </thead>
        <tbody>
          {campaigns.map((campaign) => (
            <tr key={campaign.id}>
              <td>{campaign.id}</td>
              <td>{campaign.name}</td>
              <td>{campaign.owner}</td>
              <td>
                <StatusBadge value={campaign.status} />
              </td>
              <td>{formatNumber(campaign.eligible)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
