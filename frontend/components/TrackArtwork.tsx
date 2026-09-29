import { useState } from "react";

export function TrackArtwork({
  src,
  alt,
  color,
  className,
}: {
  src?: string;
  alt: string;
  color: string;
  className: string;
}) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return <div className={className} style={{ background: color }} aria-label={alt} role="img" />;
  }

  return (
    <img
      className={`${className} object-cover`}
      src={src}
      alt={alt}
      loading="lazy"
      onError={() => setFailed(true)}
    />
  );
}
