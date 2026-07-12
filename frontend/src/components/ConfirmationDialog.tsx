import { type KeyboardEvent, type ReactNode, useEffect, useRef } from "react";

export type ConfirmationDialogProps = {
  id: string;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  confirmVariant?: "primary" | "danger";
  busy?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
};

export function ConfirmationDialog({
  id,
  title,
  description,
  confirmLabel,
  cancelLabel = "Cancel",
  confirmVariant = "danger",
  busy = false,
  onCancel,
  onConfirm,
}: ConfirmationDialogProps) {
  const headingId = `${id}-heading`;
  const descriptionId = `${id}-description`;
  const cancelButtonRef = useRef<HTMLButtonElement>(null);
  const confirmButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!busy) {
      cancelButtonRef.current?.focus();
    }
  }, [busy]);

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Escape" && !busy) {
      event.preventDefault();
      onCancel();
      return;
    }

    if (event.key !== "Tab") {
      return;
    }

    const focusableButtons = [cancelButtonRef.current, confirmButtonRef.current].filter(
      (button): button is HTMLButtonElement => button != null && !button.disabled,
    );
    if (focusableButtons.length === 0) {
      return;
    }

    const firstButton = focusableButtons[0];
    const lastButton = focusableButtons[focusableButtons.length - 1];

    if (event.shiftKey && document.activeElement === firstButton) {
      event.preventDefault();
      lastButton.focus();
    } else if (!event.shiftKey && document.activeElement === lastButton) {
      event.preventDefault();
      firstButton.focus();
    }
  }

  return (
    <div className="modal-backdrop">
      <div
        className="modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby={headingId}
        aria-describedby={descriptionId}
        onKeyDown={handleKeyDown}
      >
        <div className="section-heading">
          <h2 id={headingId}>{title}</h2>
        </div>
        <div className="confirmation-dialog-body" id={descriptionId}>
          {description}
        </div>
        <div className="button-row">
          <button
            type="button"
            className="secondary-button"
            ref={cancelButtonRef}
            disabled={busy}
            onClick={onCancel}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            className={confirmVariant === "danger" ? "danger-button" : undefined}
            ref={confirmButtonRef}
            disabled={busy}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
