-- เพิ่มตัวเลือก "นับรวมในยอดเงินคงเหลือรวม" ของแต่ละกระเป๋า
-- รันครั้งเดียวใน Supabase > SQL Editor (ที่ SQL Snippet ไม่ใช่ Logs)
alter table public.wallets add column if not exists include_in_total boolean not null default true;
