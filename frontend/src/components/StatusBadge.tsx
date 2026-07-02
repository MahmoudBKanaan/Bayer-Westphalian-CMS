interface StatusBadgeProps {
  value: string;
}

export function StatusBadge({ value }: StatusBadgeProps) {
  const tone = value.toLowerCase().replaceAll(" ", "-");
  return <span className={`status-badge ${tone}`}>{value}</span>;
}
