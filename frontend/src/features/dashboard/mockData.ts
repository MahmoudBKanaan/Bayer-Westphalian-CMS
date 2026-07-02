import type { Campaign, Customer, Product } from "@/types/domain";

export const customers: Customer[] = [
  {
    id: "C-1001",
    name: "Amina Haddad",
    segment: "Beneficiary 18-35",
    status: "Eligible",
    city: "Riyadh",
    ownedProducts: 1,
  },
  {
    id: "C-1002",
    name: "Omar Salim",
    segment: "Life payout family",
    status: "Needs consent",
    city: "Jeddah",
    ownedProducts: 2,
  },
  {
    id: "C-1003",
    name: "Lina Mansour",
    segment: "Product renewal",
    status: "Do not contact",
    city: "Dammam",
    ownedProducts: 3,
  },
];

export const campaigns: Campaign[] = [
  {
    id: "CMP-001",
    name: "Grandchild Education Plan",
    owner: "Campaign Manager",
    status: "Submitted",
    audience: 1240,
    eligible: 982,
    excluded: 258,
  },
  {
    id: "CMP-002",
    name: "Policy Expiration Reminder",
    owner: "Campaign Manager",
    status: "Approved",
    audience: 640,
    eligible: 601,
    excluded: 39,
  },
  {
    id: "CMP-003",
    name: "Investment Follow-up",
    owner: "Campaign Manager",
    status: "Draft",
    audience: 430,
    eligible: 391,
    excluded: 39,
  },
];

export const products: Product[] = [
  {
    id: "P-101",
    name: "Life Insurance",
    category: "Protection",
    activePolicies: 1520,
    expirationCampaigns: 3,
  },
  {
    id: "P-102",
    name: "Homeowner Insurance",
    category: "Property",
    activePolicies: 814,
    expirationCampaigns: 2,
  },
  {
    id: "P-103",
    name: "Investment Savings",
    category: "Investment",
    activePolicies: 602,
    expirationCampaigns: 1,
  },
];

export const performance = [
  { month: "Jan", sent: 320, conversions: 24 },
  { month: "Feb", sent: 420, conversions: 36 },
  { month: "Mar", sent: 520, conversions: 51 },
  { month: "Apr", sent: 610, conversions: 63 },
  { month: "May", sent: 730, conversions: 78 },
  { month: "Jun", sent: 820, conversions: 96 },
];
