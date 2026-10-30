-- เพิ่มลำดับของหมวดหมู่ (ใช้กับฟีเจอร์ลากจัดลำดับในหน้าเพิ่มรายการ)
-- รันครั้งเดียวใน Supabase > SQL Editor ก่อนใช้แอปเวอร์ชันใหม่
alter table public.categories add column if not exists sort_order integer not null default 0;

-- ตั้งลำดับเริ่มต้นให้หมวดเดิม ตามลำดับที่เคยแสดง (แยกตามผู้ใช้และประเภท)
with r as (
  select id, row_number() over (partition by user_id, type order by created_at, id) - 1 as rn
  from public.categories
)
update public.categories c set sort_order = r.rn from r where c.id = r.id;
