"use client";

import {
  motion,
  type Target,
  type TargetAndTransition,
  type Transition,
  type VariantLabels,
} from "motion/react";

export type TextRollProps = {
  children: string;
  duration?: number;
  getEnterDelay?: (index: number) => number;
  getExitDelay?: (index: number) => number;
  className?: string;
  transition?: Transition;
  variants?: {
    enter: {
      initial: Target | VariantLabels | boolean;
      animate: TargetAndTransition | VariantLabels;
    };
    exit: {
      initial: Target | VariantLabels | boolean;
      animate: TargetAndTransition | VariantLabels;
    };
  };
  onAnimationComplete?: () => void;
};

const defaultVariants = {
  enter: {
    initial: { rotateX: 0, y: 0, opacity: 1 },
    animate: { rotateX: 82, y: "-0.16em", opacity: 0 },
  },
  exit: {
    initial: { rotateX: -82, y: "0.16em", opacity: 0 },
    animate: { rotateX: 0, y: 0, opacity: 1 },
  },
} as const;

export function TextRoll({
  children,
  duration = 0.65,
  getEnterDelay = (index) => index * 0.035,
  getExitDelay = (index) => index * 0.035 + 0.14,
  className,
  transition = { ease: "easeOut" },
  variants,
  onAnimationComplete,
}: TextRollProps) {
  const letters = children.split("");

  return (
    <span className={className}>
      {letters.map((letter, index) => {
        const content = letter === " " ? "\u00A0" : letter;

        return (
          <span
            key={`${letter}-${index}`}
            className="relative inline-block [perspective:10000px] [transform-style:preserve-3d]"
            aria-hidden="true"
          >
            <motion.span
              className="absolute inline-block [backface-visibility:hidden] [transform-origin:50%_25%]"
              initial={variants?.enter.initial ?? defaultVariants.enter.initial}
              animate={variants?.enter.animate ?? defaultVariants.enter.animate}
              transition={{ ...transition, duration, delay: getEnterDelay(index) }}
            >
              {content}
            </motion.span>
            <motion.span
              className="absolute inline-block [backface-visibility:hidden] [transform-origin:50%_100%]"
              initial={variants?.exit.initial ?? defaultVariants.exit.initial}
              animate={variants?.exit.animate ?? defaultVariants.exit.animate}
              transition={{ ...transition, duration, delay: getExitDelay(index) }}
              onAnimationComplete={letters.length === index + 1 ? onAnimationComplete : undefined}
            >
              {content}
            </motion.span>
            <span className="invisible">{content}</span>
          </span>
        );
      })}
      <span className="sr-only">{children}</span>
    </span>
  );
}
