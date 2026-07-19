import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, Downloads, Me, startGoogleLogin } from "../api";
import { getLanguage, setLanguage, t, Language } from "../i18n";

type Props = {
  me: Me | null;
  onLanguageChange: () => void;
};

export function LandingPage({ me, onLanguageChange }: Props) {
  const [downloads, setDownloads] = useState<Downloads | null>(null);

  useEffect(() => {
    api.getDownloads().then(setDownloads).catch(() => {
      setDownloads({
        windowsUrl: "#",
        macosUrl: "#",
        linuxUrl: "#"
      });
    });
  }, []);

  const language = getLanguage();

  return (
    <div className="page hero">
      <div className="topbar">
        <div />
        <div className="lang-switch">
          {(["en", "uk"] as Language[]).map((item) => (
            <button
              key={item}
              className={language === item ? "active" : ""}
              onClick={() => {
                setLanguage(item);
                onLanguageChange();
              }}
            >
              {item.toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      <p className="muted" style={{ margin: 0, letterSpacing: "0.08em", textTransform: "uppercase" }}>
        {t("landing.tagline")}
      </p>
      <h1 className="brand">{t("ui.app.title")}</h1>
      <p>{t("landing.subtitle")}</p>

      <div className="actions">
        {me ? (
          <Link className="btn btn-primary" to={me.onboarded ? "/org" : "/onboarding"}>
            {t("landing.openApp")}
          </Link>
        ) : (
          <button className="btn btn-primary" onClick={startGoogleLogin}>
            {t("landing.login")}
          </button>
        )}
      </div>

      <div>
        <h3 style={{ marginBottom: "0.5rem" }}>{t("landing.download")}</h3>
        <div className="download-row">
          <a className="btn btn-secondary" href={downloads?.windowsUrl ?? "#"}>
            {t("landing.windows")}
          </a>
          <a className="btn btn-secondary" href={downloads?.macosUrl ?? "#"}>
            {t("landing.macos")}
          </a>
          <a className="btn btn-secondary" href={downloads?.linuxUrl ?? "#"}>
            {t("landing.linux")}
          </a>
        </div>
      </div>
    </div>
  );
}
