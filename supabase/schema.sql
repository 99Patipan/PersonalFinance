-- โครงสร้างฐานข้อมูลของ PersonalFinance บน Supabase (ตรงกับที่รันไว้ในโปรเจกต์ personal-finance)
create table public.wallets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null,
  color bigint not null,
  initial_cents bigint not null default 0,
  created_at timestamptz not null default now()
);

create table public.categories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null,
  type text not null check (type in ('INCOME','EXPENSE')),
  emoji text not null default '',
  sort_order integer not null default 0,
  created_at timestamptz not null default now()
);

create table public.transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  type text not null check (type in ('INCOME','EXPENSE','TRANSFER')),
  amount_cents bigint not null,
  category_id uuid references public.categories(id) on delete set null,
  wallet_id uuid not null references public.wallets(id) on delete restrict,
  to_wallet_id uuid references public.wallets(id) on delete restrict,
  timestamp_ms bigint not null,
  detail text not null default '',
  note text not null default '',
  receipt_path text,
  created_at timestamptz not null default now()
);

create table public.budgets (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  category_id uuid not null references public.categories(id) on delete cascade,
  limit_cents bigint not null,
  primary key (user_id, category_id)
);

create index wallets_user_idx on public.wallets(user_id);
create index categories_user_idx on public.categories(user_id);
create index transactions_user_ts_idx on public.transactions(user_id, timestamp_ms desc);
create index transactions_wallet_idx on public.transactions(wallet_id);
create index transactions_to_wallet_idx on public.transactions(to_wallet_id);
create index transactions_category_idx on public.transactions(category_id);
create index budgets_category_idx on public.budgets(category_id);

alter table public.wallets enable row level security;
alter table public.categories enable row level security;
alter table public.transactions enable row level security;
alter table public.budgets enable row level security;

create policy "own wallets" on public.wallets for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "own categories" on public.categories for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "own transactions" on public.transactions for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "own budgets" on public.budgets for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

-- ที่เก็บรูปใบเสร็จ (ส่วนตัว) โฟลเดอร์แรกของ path ต้องเป็น user id ของเจ้าของ
insert into storage.buckets (id, name, public) values ('receipts', 'receipts', false) on conflict (id) do nothing;

create policy "own receipts read" on storage.objects for select to authenticated
  using (bucket_id = 'receipts' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "own receipts insert" on storage.objects for insert to authenticated
  with check (bucket_id = 'receipts' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "own receipts update" on storage.objects for update to authenticated
  using (bucket_id = 'receipts' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "own receipts delete" on storage.objects for delete to authenticated
  using (bucket_id = 'receipts' and (storage.foldername(name))[1] = (select auth.uid())::text);
