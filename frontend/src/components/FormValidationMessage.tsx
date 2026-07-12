export type FormValidationMessageProps = {
  id?: string;
  message?: string;
};

export function FormValidationMessage({ id, message }: FormValidationMessageProps) {
  if (message == null || message.length === 0) {
    return null;
  }

  return (
    <span className="field-error" id={id}>
      {message}
    </span>
  );
}
