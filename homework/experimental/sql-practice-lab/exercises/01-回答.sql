# 查出所有上架（status=1）的 SPU 的 id, name, brand
select id, name, brand from spu where status = 1;

#查 spu 里品牌是 Apple 或 Xiaomi 的商品，只要 name, brand

select name, brand from spu where brand = 'Apple' or brand = 'Xiaomi';

select name, brand from spu where brand in ('Apple', 'Xiaomi');

#查 sku 里价格在 1000 到 5000 之间的记录，按价格从低到高排。

select * from sku where price between 1000 and 5000 order by price desc; # 高到低

select * from sku where price between 1000 and 5000 order by price; # 低到高， asc 默认升序


# 查最贵的 3 个 SKU 的 sku_code, price

select sku_code, price from sku order by price desc limit 3;

# spu 表里一共有几个不同的品牌？
select count(distinct brand) as '品牌数' from spu;

# 统计每个 spu 有几个 SKU，输出 spu_id, sku 数量

select spu_id, count(*) as 'sku 数量' from sku group by spu_id;


# 每个 spu 的最低价、最高价、平均价（保留 2 位小数）


select spu_id, round(min(price), 2) as '最低价' from sku group by spu_id;
select spu_id, round(max(price), 2) as '最高价' from sku group by spu_id;
select spu_id, round(avg(price), 2) as '平均价' from sku group by spu_id;


# 找出拥有 2 个及以上 SKU 的 spu_id。

select sku.spu_id, count(*) as 'sku 数量' from sku group by spu_id having count(*) > 2;

#每个品牌有几个 SPU，按数量倒序
select brand, count(*) as 'SPU 数量' from spu group by brand order by count(*) desc;

#表所有库存加起来是多少？平均库存多少？

select sum(sku.stock) as '总库存' from sku;


select avg(sku.stock) as '平均库存' from sku;


# 列出每个 SKU 属于哪个 SPU：输出 sku_code, spu 名称, price。

select sku.sku_code, spu.name as 'spu 名称', sku.price from sku join spu on sku.spu_id = spu.id;


#列出每个 SPU 属于哪个分类：输出 spu 名称, 分类名称

select spu.name as 'spu 名称', category.name as '分类名称' from spu inner join category on spu.category_id = category.id;

# 哪些 SPU 一个 SKU 都没有？（用 LEFT JOIN 找"右表为 NULL"）

select spu.name, 0 as 'sku 数量' from spu left join sku on spu.id = sku.spu_id where sku.spu_id is null;


# 三表连起来：输出 分类名, 商品名, sku_code, price，只看上架 SPU。
select category.name as '分类名', spu.name as '商品名', sku.sku_code, sku.price from category
    inner join spu on category.id = spu.category_id
    inner join sku on spu.id = sku.spu_id where spu.status = 1;

# 每个分类下有多少个 SPU（含 0 个的分类也要显示）

select category.name, count(spu.id) from category
     left join spu on category.id = spu.category_id group by category.id;



# 查价格高于全表 SKU 平均价的 SKU。
select * from sku where price > (select avg(price) from sku);


# 查"至少有一个 SKU 价格超过 8000"的 SPU 名称。


select distinct spu.name as 'SPU 名称'
from spu inner join sku on spu.id = sku.spu_id where sku.price > 8000; # 联表

select spu.name as 'SPU 名称'
from spu inner join sku on spu.id = sku.spu_id where sku.price > 8000 group by spu.name; # 联表但是用group 去除


select spu.name from spu where exists( select sku.id from sku where sku.spu_id =spu.id and price > 8000);  # exists 函数


select spu.name from spu where spu.id in (select sku.spu_id from sku where sku.price > 8000); # in  写法



# 查每个 SPU 及其"最高价 SKU 的价格"
select sub.name, max(sub.price) as '最高价 SKU 的价格' from
   (select  spu.name ,  sku.price  from spu inner join sku on spu.id = sku.spu_id) sub  group by sub.name;

# 上面的可以简化
select spu.id, spu.name, max(sku.price) from spu inner join sku on spu.id = sku.spu_id group by spu.id, spu.name;



# 给「小米 14」(spu_id=2) 新增一个 SKU：sku_code='MI14-1T-BLK'、价格 5499、库存 50、规格 {"容量":"1T","颜色":"黑色"}

insert into sku (spu_id, sku_code, price, stock, spec_json, status) VALUE (
    2, 'MI14-1T-BLK', 5499, 50, '{"容量":"1T","颜色":"黑色"}', 1
    );

# 把所有 Xiaomi 品牌 SPU 下的 SKU 统一涨价 5%（price = price * 1.05）

# 先查询当前的原价

select price as '原价' from sku where spu_id in
                              (select id from spu where brand = 'Xiaomi');

# 涨价 5%， update  然后使用相同的where 语句， 忘记执行几次了
update sku set price = round(price * 1.05, 2)  where spu_id in
                                                     (select id from spu where brand = 'Xiaomi');

select price as '现在的价格' from sku where spu_id in
                                      (select id from spu where brand = 'Xiaomi');


# 题 21　把下架且无 SKU 的样机 id=7 删除。

delete from spu where id = 7;

# 同步删除他下面的sku,  没有物理外键， 代码逻辑同步删除

delete from sku where spu_id = 7;


# 软删除）　把 id=5（快充数据线）做软删除：不真删，而是把 deleted_at 置为当前时间。

update spu set deleted_at = now() where id = 5;

# 同样软删除它下面的sku, 用该和上面用同一个now() 的值

update sku set deleted_at = now() where spu_id = 5;