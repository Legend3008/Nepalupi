export default function LoadingSpinner({ size = 'md', color = 'primary', text }) {
  const sizes = { sm: 'w-5 h-5', md: 'w-8 h-8', lg: 'w-12 h-12', xl: 'w-16 h-16' };
  const colors = {
    primary: 'border-nupi-primary',
    white: 'border-white',
    accent: 'border-nupi-accent',
  };

  return (
    <div className="flex flex-col items-center justify-center gap-3">
      <div className={`${sizes[size]} border-4 ${colors[color]} border-t-transparent rounded-full animate-spin`} />
      {text && <p className="text-sm text-nupi-text-secondary font-medium">{text}</p>}
    </div>
  );
}

export function FullPageLoader({ text = 'Loading...' }) {
  return (
    <div className="fixed inset-0 bg-white/90 z-[100] flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <div className="w-12 h-12 border-4 border-nupi-primary border-t-transparent rounded-full animate-spin" />
        <p className="text-nupi-text-secondary font-medium">{text}</p>
      </div>
    </div>
  );
}

export function SkeletonCard({ lines = 3 }) {
  return (
    <div className="card animate-pulse">
      <div className="flex items-center gap-3 mb-3">
        <div className="w-10 h-10 rounded-full bg-gray-200" />
        <div className="flex-1">
          <div className="h-4 bg-gray-200 rounded w-3/4 mb-2" />
          <div className="h-3 bg-gray-200 rounded w-1/2" />
        </div>
      </div>
      {Array.from({ length: lines - 1 }, (_, i) => (
        <div key={i} className={`h-3 bg-gray-200 rounded mb-2 w-${['full', '5/6', '4/6', '3/4'][i % 4]}`} />
      ))}
    </div>
  );
}
