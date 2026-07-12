import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ConfirmationDialog } from "@/components/ConfirmationDialog";

describe("ConfirmationDialog", () => {
  it("requires an explicit confirm action for sensitive operations", async () => {
    const onCancel = vi.fn();
    const onConfirm = vi.fn();

    render(
      <ConfirmationDialog
        id="disable-user-confirmation"
        title="Confirm user disable"
        description={<p>This user will no longer be able to access the platform.</p>}
        confirmLabel="Disable user"
        onCancel={onCancel}
        onConfirm={onConfirm}
      />,
    );

    const dialog = screen.getByRole("dialog", { name: "Confirm user disable" });
    expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(
      screen.getByText("This user will no longer be able to access the platform."),
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole("button", { name: "Disable user" }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("supports keyboard cancellation and keeps focus inside the dialog", async () => {
    const onCancel = vi.fn();
    const onConfirm = vi.fn();
    const user = userEvent.setup();

    render(
      <ConfirmationDialog
        id="launch-confirmation"
        title="Confirm campaign launch"
        description={<p>This will send the campaign to eligible recipients.</p>}
        confirmLabel="Confirm launch"
        onCancel={onCancel}
        onConfirm={onConfirm}
      />,
    );

    const dialog = screen.getByRole("dialog", { name: "Confirm campaign launch" });
    const cancelButton = screen.getByRole("button", { name: "Cancel" });
    const confirmButton = screen.getByRole("button", { name: "Confirm launch" });

    expect(dialog).toHaveAttribute("aria-describedby", "launch-confirmation-description");
    expect(cancelButton).toHaveFocus();

    await user.keyboard("{Shift>}{Tab}{/Shift}");
    expect(confirmButton).toHaveFocus();

    await user.tab();
    expect(cancelButton).toHaveFocus();

    await user.keyboard("{Escape}");
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
