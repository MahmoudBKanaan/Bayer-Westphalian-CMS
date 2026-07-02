export type CampaignStatus = "Draft" | "Submitted" | "Approved" | "Active" | "Paused" | "Completed";

export type CustomerStatus = "Eligible" | "Needs consent" | "Do not contact";

export interface Customer {
  id: string;
  name: string;
  segment: string;
  status: CustomerStatus;
  city: string;
  ownedProducts: number;
}

export interface Campaign {
  id: string;
  name: string;
  owner: string;
  status: CampaignStatus;
  audience: number;
  eligible: number;
  excluded: number;
}

export interface Product {
  id: string;
  name: string;
  category: string;
  activePolicies: number;
  expirationCampaigns: number;
}
