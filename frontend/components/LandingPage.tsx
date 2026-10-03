import Link from "next/link";
import { GithubIcon, LinkedInIcon, MicIcon, UploadIcon } from "@/components/icons";
import ImageStreamHero from "@/components/ImageStreamHero";
import { TextRoll } from "@/components/TextRoll";
import landingCovers from "@/lib/landing-covers.json";

type LandingTrack = (typeof landingCovers)[number];

const FAQ_QUESTIONS = [
  "What is HashTune?",
  "How does HashTune identify a song?",
  "Can I identify a track from a microphone recording?",
  "Can I upload an audio clip instead?",
  "How long does an audio sample need to be?",
  "Which audio formats does HashTune support?",
];

function GradientBackground() {
  return (
    <div aria-hidden="true" className="absolute inset-0 z-0 h-full w-full overflow-hidden">
      <div
        className="absolute inset-0"
        style={{
          backgroundColor: "#123A6B",
          backgroundImage:
            "url(\"data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='120' height='120'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' stitchTiles='stitch'/></filter><rect width='100%' height='100%' filter='url(%23n)' opacity='0.215'/></svg>\"), radial-gradient(150% 48.4% at 41.58% 6%, rgba(234, 247, 251, 0.92) 0%, rgba(234, 247, 251, 0) 53%), radial-gradient(150% 48.4% at 42.42% 33%, rgba(127, 198, 230, 0.92) 0%, rgba(127, 198, 230, 0) 53%), radial-gradient(150% 48.4% at 51.19% 67%, rgba(46, 124, 192, 0.92) 0%, rgba(46, 124, 192, 0) 53%), radial-gradient(150% 48.4% at 53.67% 94%, rgba(18, 58, 107, 0.92) 0%, rgba(18, 58, 107, 0) 53%)",
          backgroundSize: "7.5rem 7.5rem, auto, auto, auto, auto",
          backgroundBlendMode: "overlay, normal, normal, normal, normal",
        }}
      />
      <svg className="absolute inset-0 h-full w-full opacity-[.215] mix-blend-overlay">
        <filter id="landing-grain">
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.8"
            numOctaves="2"
            stitchTiles="stitch"
          />
          <feColorMatrix type="saturate" values="0" />
        </filter>
        <rect width="100%" height="100%" filter="url(#landing-grain)" />
      </svg>
      <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-[#09090b]" />
    </div>
  );
}

function FloatingCover({ track, side }: { track: LandingTrack; side: "left" | "right" }) {
  const isLeft = side === "left";

  return (
    <div
      className={`absolute hidden lg:block animate-float-subtle ${isLeft ? "left-[5%]" : "right-[5%]"}`}
      style={{ 
        top: isLeft ? "40%" : "45%",
        animationDelay: isLeft ? "0s" : "-3s" 
      }}
    >
      <div
        className={`-translate-y-1/2 cursor-pointer shadow-2xl transition-[transform] duration-500 hover:scale-105 ${isLeft ? "-rotate-12 hover:-rotate-6" : "rotate-12 hover:rotate-6"}`}
      >
        <div
          className={`${isLeft ? "h-56 w-56" : "h-64 w-64"} group relative overflow-hidden rounded-xl border border-white/20 bg-black`}
        >
          <img
            src={track.cover}
            alt={`${track.title} cover art`}
            className="absolute inset-0 h-full w-full object-cover transition-transform duration-700 group-hover:scale-110"
          />
          <div className="absolute inset-0 flex items-center justify-center bg-black/20 opacity-0 transition-opacity group-hover:opacity-100">
            <div
              className={`${isLeft ? "size-12" : "size-14"} grid place-items-center rounded-full border border-white/30 bg-black/60 backdrop-blur-md`}
            >
              <span
                className={`${isLeft ? "border-l-[0.625rem] border-y-[0.375rem]" : "border-l-[0.75rem] border-y-[0.5rem]"} ml-1 h-0 w-0 border-y-transparent border-l-white`}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function LandingHeader() {
  return (
    <div className="relative z-50 -mb-[5rem] sticky top-0 border-b border-white/[0.08] bg-black/[0.16] text-white backdrop-blur-md backdrop-brightness-90">
      <nav className="mx-auto flex w-full max-w-[87.5rem] items-center justify-between px-6 py-4 sm:px-8 sm:py-[1.125rem] lg:py-5">
        <Link href="/" className="flex items-center gap-3">
          <img
            src="/hashtune-logo-white.svg"
            alt="HashTune"
            className="size-6 drop-shadow-md sm:size-7"
          />
          <span className="text-[clamp(1rem,1.1vw,1.125rem)] font-bold tracking-tight drop-shadow-md">
            HashTune
          </span>
        </Link>
        <div className="flex items-center gap-5 text-[clamp(0.875rem,1vw,1rem)] font-medium text-white/90 drop-shadow-md sm:gap-7">
          <Link href="#features" className="hidden transition-colors hover:text-white sm:block">
            Features
          </Link>
          <Link href="#faq" className="hidden transition-colors hover:text-white sm:block">
            FAQ
          </Link>
          <Link
            href="/app"
            className="rounded-full bg-white px-4 py-2 text-[clamp(0.875rem,1vw,1rem)] font-bold text-black shadow-lg transition-colors hover:bg-white/90 sm:px-5 sm:py-2.5"
          >
            Open App
          </Link>
        </div>
      </nav>
    </div>
  );
}

function LandingHero({ tracks }: { tracks: LandingTrack[] }) {
  return (
    <section className="relative flex min-h-[90vh] flex-col overflow-hidden">
      <GradientBackground />

      <div className="relative z-10 flex flex-1 flex-col items-center justify-center px-4 pb-32 pt-12 text-center">
        {tracks[0] && <FloatingCover track={tracks[0]} side="left" />}
        {tracks[1] && <FloatingCover track={tracks[1]} side="right" />}
        <h1 className="max-w-[50rem] text-[clamp(3rem,6vw,5.5rem)] font-semibold leading-[1.05] tracking-tight text-white drop-shadow-xl">
          <TextRoll className="inline-block" transition={{ ease: [0.22, 1, 0.36, 1] }}>
            Turn a sound into a discovery
          </TextRoll>
        </h1>
        <p className="mt-6 max-w-[31.25rem] text-lg font-medium text-white/90 drop-shadow-md">
          Capture a song you do not recognize. HashTune brings the matching track, artwork, and
          metadata into focus.
        </p>
        <div className="relative z-20 mt-12 flex w-full max-w-[37.5rem] flex-col items-center gap-3 rounded-2xl border border-white/20 bg-black/40 p-4 shadow-2xl backdrop-blur-xl sm:flex-row">
          <Link
            href="/app"
            className="flex h-14 w-full flex-1 items-center justify-center gap-3 rounded-xl border border-white/10 bg-white/10 text-[0.9375rem] font-semibold transition-colors hover:bg-white/20"
          >
            <MicIcon className="size-5" />
            Listen to Microphone
          </Link>
          <Link
            href="/app"
            className="flex h-14 w-full flex-1 items-center justify-center gap-3 rounded-xl bg-[#3B82F6] text-[0.9375rem] font-semibold text-white shadow-[0_0_1.25rem_rgba(59,130,246,0.3)] transition-colors hover:bg-[#2563EB]"
          >
            <UploadIcon className="size-5" />
            Upload Audio File
          </Link>
        </div>
      </div>
    </section>
  );
}

function CoverStream({ tracks }: { tracks: LandingTrack[] }) {
  return (
    <section className="relative z-10 overflow-hidden border-t border-white/5 bg-[#09090b] py-16 sm:py-24">
      <div className="mx-auto max-w-[87.5rem] px-6 text-center">
        <p className="text-[0.6875rem] font-bold uppercase tracking-[0.2em] text-[#73baff]">
          Music, made searchable
        </p>
        <h2 className="mt-4 text-[clamp(2rem,4vw,3.5rem)] font-semibold leading-tight tracking-tight">
          Every track has a fingerprint.
        </h2>
        <ImageStreamHero
          images={tracks.map((track) => ({ src: track.cover }))}
          cards={10}
          speed={22}
          className="mx-auto mt-8 h-[min(58vw,38.75rem)] w-full max-w-[87.5rem]"
          path={{ cardWidth: 15, cardHeight: 21, exitHeight: 40, railExit: 42 }}
        />
      </div>
    </section>
  );
}

function FeatureSection() {
  return (
    <section id="features" className="bg-[#09090b] px-6 py-24">
      <div className="mx-auto max-w-[75rem]">
        <h2 className="mb-16 text-center text-[clamp(2rem,4vw,3.5rem)] font-semibold leading-tight tracking-tight">
          Make every recognition useful
        </h2>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:auto-rows-[18.75rem] lg:grid-cols-4">
          <article className="relative overflow-hidden rounded-3xl border border-white/5 bg-[#111113] p-8 lg:col-span-2">
            <div className="relative z-10 w-2/3">
              <h3 className="mb-3 text-2xl font-bold">Recognize what you hear</h3>
              <p className="text-[0.9375rem] leading-relaxed text-white/50">
                Record a snippet or upload a clip. HashTune compares its fingerprint with indexed
                tracks and returns the closest match.
              </p>
            </div>
          </article>
          <article className="relative overflow-hidden rounded-3xl border border-white/5 bg-[#111113] p-8">
            <h3 className="mb-3 text-xl font-bold">See the full picture</h3>
            <p className="text-[0.9375rem] leading-relaxed text-white/50">
              Open a match with its title, artist, album, cover art, format, and release details in
              one place.
            </p>
          </article>
          <article className="relative overflow-hidden rounded-3xl border border-white/5 bg-[#111113] p-8">
            <h3 className="mb-3 text-xl font-bold">Find your next track</h3>
            <p className="text-[0.9375rem] leading-relaxed text-white/50">
              Explore the indexed library, filter by artist or album, and open any result to view
              its artwork and metadata.
            </p>
          </article>
        </div>
      </div>
    </section>
  );
}

function AppBanner() {
  return (
    <section className="relative overflow-hidden px-6 py-32">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-[#1e3a8a]/20 via-[#09090b]/0 to-transparent" />
      <div className="relative z-10 mx-auto max-w-[50rem] text-center">
        <h2 className="mb-6 text-[clamp(3rem,6vw,5rem)] font-bold leading-none tracking-tight">
          Your music, ready to explore
        </h2>
        <p className="mb-12 text-lg text-white/60">
          Identify a sound, uncover the track, and keep every discovery close at hand.
        </p>
        <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
          <ActionCard
            title="Recognize"
            description="Turn a sound into a match"
            action="Open the app"
          />
          <ActionCard
            title="Explore"
            description="Browse every match and detail"
            action="Explore the library"
          />
        </div>
      </div>
    </section>
  );
}

function ActionCard({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action: string;
}) {
  return (
    <div className="w-full rounded-2xl border border-white/10 bg-white/5 p-6 text-left transition-colors hover:bg-white/10 sm:w-64">
      <div className="mb-2 flex items-center gap-2">
        <span className="text-2xl font-bold">{title}</span>
        <span className="text-[#3B82F6]">★</span>
      </div>
      <p className="mb-4 text-xs text-white/50">{description}</p>
      <Link
        href="/app"
        className="block w-full rounded-full bg-white py-2 text-[0.8125rem] font-bold text-black"
      >
        {action}
      </Link>
    </div>
  );
}

function FaqSection() {
  return (
    <section id="faq" className="border-t border-white/5 px-6 py-24">
      <div className="mx-auto max-w-[50rem]">
        <div className="mb-16 text-center">
          <h2 className="text-[clamp(2rem,4vw,3.5rem)] font-semibold tracking-tight">
            Frequently asked questions
          </h2>
          <p className="mt-4 text-base text-white/50">
            Everything you need to know about HashTune.
          </p>
        </div>
        <div className="space-y-4">
          {FAQ_QUESTIONS.map((question) => (
            <div key={question} className="border-b border-white/10 pb-4">
              <button className="flex w-full items-center justify-between py-4 text-left transition-colors hover:text-white/70">
                <span className="text-base font-medium">{question}</span>
                <span className="text-xl font-light">+</span>
              </button>
            </div>
          ))}
          <div className="pt-8 text-center">
            <a href="#faq" className="text-sm text-white/50 transition-colors hover:text-white">
              See the full FAQ →
            </a>
          </div>
          <div className="flex justify-center pt-12">
            <Link
              href="/app"
              className="inline-block rounded-full bg-[#3B82F6] px-8 py-3 text-sm font-bold transition-colors hover:bg-[#2563EB]"
            >
              Open HashTune
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="mx-auto max-w-[87.5rem] border-t border-white/5 px-12 pt-24">
      <div className="mb-16 grid grid-cols-2 gap-12 md:grid-cols-3">
        <FooterColumn title="Brand" links={["About", "About Author"]} />
        <FooterColumn title="Products" links={["Desktop App", "Web Utility", "CLI Tool"]} />
        <FooterColumn title="Legal" links={["Terms of Service", "Privacy Policy"]} />
      </div>
      <div className="flex flex-col items-center justify-between border-t border-white/10 py-6 text-xs text-white/30 md:flex-row">
        <p>
          © {new Date().getFullYear()} HashTune <span className="text-white/20">· Built by </span>
          <a
            href="https://norbertfila.com"
            target="_blank"
            rel="noreferrer"
            className="font-semibold text-[#73baff] transition-colors hover:text-white"
          >
            Norbert Fila
          </a>
        </p>
        <div className="mt-4 flex items-center gap-4 md:mt-0">
          <a
            href="https://www.linkedin.com/in/norbert-fila/"
            target="_blank"
            rel="noreferrer"
            aria-label="Norbert Fila on LinkedIn"
            className="text-white/40 transition-colors hover:text-white"
          >
            <LinkedInIcon className="size-4" />
          </a>
          <a
            href="https://github.com/Nubet/HashTune"
            target="_blank"
            rel="noreferrer"
            aria-label="HashTune on GitHub"
            className="text-white/40 transition-colors hover:text-white"
          >
            <GithubIcon className="size-4" />
          </a>
        </div>
      </div>
    </footer>
  );
}

function FooterColumn({ title, links }: { title: string; links: string[] }) {
  return (
    <div>
      <h4 className="mb-4 text-sm font-bold">{title}</h4>
      <ul className="space-y-3 text-[0.8125rem] text-white/50">
        {links.map((link) => (
          <li key={link}>
            <a href="#" className="transition-colors hover:text-white">
              {link}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function LandingPage() {
  return (
    <main className="min-h-screen bg-[#09090b] pb-24 font-sans text-white selection:bg-[#3B82F6] selection:text-white">
      <LandingHeader />
      <LandingHero tracks={landingCovers} />
      <CoverStream tracks={landingCovers} />
      <FeatureSection />
      <AppBanner />
      <FaqSection />
      <Footer />
    </main>
  );
}
