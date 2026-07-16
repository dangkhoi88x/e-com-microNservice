import { cn } from "@/lib/utils";

function PageHeader({ title, description, eyebrow = "Workspace", actions, className }) {
  return (
    <div
      className={cn(
        "admin-section-header flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between",
        className,
      )}
    >
      <div className="min-w-0">
        <div className="admin-header-eyebrow">{eyebrow}</div>
        <h1 className="font-['Space_Grotesk'] text-[34px] font-black leading-none tracking-normal text-zinc-950 sm:text-[38px]">
          {title}
        </h1>
        {description && (
          <p className="admin-header-description">{description}</p>
        )}
      </div>
      {actions && <div className="admin-header-actions">{actions}</div>}
    </div>
  );
}

export { PageHeader };
