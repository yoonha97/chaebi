export default function GuideLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return <main className="flex-1 overflow-hidden">{children}</main>
}
