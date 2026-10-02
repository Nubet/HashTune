"use client";

import * as React from "react";

type CorridorPath = {
  perspective?: number;
  cardWidth?: number;
  cardHeight?: number;
  cardRadius?: number;
  birthHeight?: number;
  exitHeight?: number;
  railBirth?: number;
  railExit?: number;
  fan?: number;
  turnBirth?: number;
  turnExit?: number;
  stops?: number;
};

type StreamImage = {
  src: string;
};

type ImageStreamHeroProps = {
  images: StreamImage[];
  cards?: number;
  speed?: number;
  axis?: number;
  path?: CorridorPath;
  className?: string;
  style?: React.CSSProperties;
};

const DEFAULT_PATH: Required<CorridorPath> = {
  perspective: 30,
  cardWidth: 18,
  cardHeight: 25,
  cardRadius: 0.4,
  birthHeight: 2.6,
  exitHeight: 46,
  railBirth: -11,
  railExit: 44,
  fan: 3.3,
  turnBirth: 6,
  turnExit: 28,
  stops: 24,
};

function createKeyframes(direction: 1 | -1, name: string, path: Required<CorridorPath>) {
  const steps: string[] = [];

  for (let step = 0; step <= path.stops; step += 1) {
    const progress = step / path.stops;
    const scale =
      (path.birthHeight / path.cardHeight) * Math.pow(path.exitHeight / path.birthHeight, progress);
    const depth = path.perspective * (1 - 1 / scale);
    const rail =
      path.railExit - (path.railExit - path.railBirth) * Math.pow(1 - progress, path.fan);
    const turn = path.turnBirth + (path.turnExit - path.turnBirth) * progress;

    steps.push(
      `${(progress * 100).toFixed(2)}%{transform:translate3d(${(direction * rail).toFixed(2)}cqw,0,${depth.toFixed(2)}cqw) rotateY(${(-direction * turn).toFixed(2)}deg)}`,
    );
  }

  return `@keyframes ${name}{${steps.join("")}}`;
}

function ImageStreamHero({
  images,
  cards = 9,
  speed = 18,
  axis = 55,
  path,
  className,
  style,
}: ImageStreamHeroProps) {
  const id = React.useId().replace(/[^a-zA-Z0-9]/g, "");
  const rightKeyframes = `hash-tune-stream-right-${id}`;
  const leftKeyframes = `hash-tune-stream-left-${id}`;
  const cardClass = `hash-tune-stream-card-${id}`;
  const corridor: Required<CorridorPath> = Object.assign({}, DEFAULT_PATH, path);

  if (images.length === 0) return null;

  const css =
    createKeyframes(1, rightKeyframes, corridor) +
    createKeyframes(-1, leftKeyframes, corridor) +
    `@media(prefers-reduced-motion:reduce){.${cardClass}{animation-play-state:paused}}`;

  return (
    <div
      className={`relative overflow-hidden ${className ?? ""}`}
      style={{ containerType: "inline-size", ...style }}
    >
      <style>{css}</style>
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0"
        style={{
          perspective: `${corridor.perspective}cqw`,
          perspectiveOrigin: `50% ${axis}%`,
        }}
      >
        <div className="absolute inset-0" style={{ transformStyle: "preserve-3d" }}>
          {[rightKeyframes, leftKeyframes].map((keyframes) =>
            Array.from({ length: cards }, (_, index) => {
              const image = images[index % images.length];

              return (
                <div
                  key={`${keyframes}-${index}`}
                  className={`${cardClass} absolute overflow-hidden shadow-[0_24px_50px_rgb(0_0_0/0.35)]`}
                  style={{
                    left: "50%",
                    top: `${axis}%`,
                    width: `${corridor.cardWidth}cqw`,
                    height: `${corridor.cardHeight}cqw`,
                    marginLeft: `${-corridor.cardWidth / 2}cqw`,
                    marginTop: `${-corridor.cardHeight / 2}cqw`,
                    borderRadius: `${corridor.cardRadius}cqw`,
                    animation: `${keyframes} ${speed}s linear infinite`,
                    animationDelay: `${-(index * speed) / cards}s`,
                    backfaceVisibility: "hidden",
                  }}
                >
                  <img
                    src={image.src}
                    alt=""
                    loading="lazy"
                    decoding="async"
                    className="h-full w-full object-cover"
                    draggable={false}
                  />
                </div>
              );
            }),
          )}
        </div>
      </div>
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-[#09090b]/75 via-transparent to-[#09090b]" />
    </div>
  );
}

export default ImageStreamHero;
