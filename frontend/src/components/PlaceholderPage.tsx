interface PlaceholderPageProps {
  title: string;
  description: string;
}

export default function PlaceholderPage({
  title,
  description,
}: PlaceholderPageProps) {
  return (
    <div>
      <h1 className="text-3xl font-bold text-slate-900">
        {title}
      </h1>

      <p className="mt-4 text-slate-600">
        {description}
      </p>
    </div>
  );
}