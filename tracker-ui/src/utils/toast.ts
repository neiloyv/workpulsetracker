export type ToastVariant = "info" | "success" | "error";

export type ToastEventDetail = {
  id: number;
  message: string;
  variant: ToastVariant;
};

const EVENT_NAME = "wpt:toast";
let counter = 0;

export function toast(message: string, variant: ToastVariant = "info"): void {
  counter += 1;
  const detail: ToastEventDetail = { id: counter, message, variant };
  window.dispatchEvent(new CustomEvent<ToastEventDetail>(EVENT_NAME, { detail }));
}

export function subscribeToast(callback: (detail: ToastEventDetail) => void): () => void {
  const handler = (event: Event) => {
    callback((event as CustomEvent<ToastEventDetail>).detail);
  };
  window.addEventListener(EVENT_NAME, handler);
  return () => window.removeEventListener(EVENT_NAME, handler);
}
