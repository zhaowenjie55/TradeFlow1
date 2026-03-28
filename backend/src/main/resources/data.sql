-- Seed data generated from docs/商品数据.csv
DELETE FROM product_detail_snapshot WHERE product_id LIKE 'cn-1688-acrylic-%';
DELETE FROM gv_platform_item_snapshot WHERE snapshot_id LIKE 'seed-snap-cn-1688-acrylic-%';
DELETE FROM gv_platform_item WHERE external_item_id LIKE 'cn-1688-acrylic-%';
DELETE FROM gv_search_run_result WHERE search_run_id LIKE 'seed-sr-%';
DELETE FROM gv_search_run WHERE search_run_id LIKE 'seed-sr-%';

INSERT INTO gv_search_run (
    search_run_id, task_id, phase, platform, query_text, status, fallback_used, error_message, created_at, updated_at
) VALUES
    ('seed-sr-acrylic-desktop-organizer', 'seed-task-acrylic-desktop-organizer', 'PHASE1', 'AMAZON', 'Acrylic Desktop Organizer', 'SUCCEEDED', FALSE, NULL, NOW(), NOW())
ON CONFLICT (search_run_id) DO UPDATE SET
    task_id = EXCLUDED.task_id,
    phase = EXCLUDED.phase,
    platform = EXCLUDED.platform,
    query_text = EXCLUDED.query_text,
    status = EXCLUDED.status,
    fallback_used = EXCLUDED.fallback_used,
    error_message = EXCLUDED.error_message,
    updated_at = NOW();

INSERT INTO gv_search_run_result (
    search_run_id, platform, external_item_id, rank_no, title, price, image, link, raw_jsonb
) VALUES
    ('seed-sr-acrylic-desktop-organizer', 'AMAZON', 'amz-acrylic-01', 1, 'BEYGORM Magnetic Dry Erase Marker Holder for Whiteboard,Non-slip Acrylic Magnet Pen Holder for Fridge Refrigerator,Locker Organizer for Work,Locker Accessories,Pencil Cup for Desk,Marker Organizer', 12.99, 'https://m.media-amazon.com/images/I/81ViBxtYFoL._AC_SL1500_.jpg', 'https://www.amazon.com/BEYGORM-Whiteboard-Refrigerator-Organizer-Accessories/dp/B0CHJNZWCR/ref=sr_1_2?crid=1IKCPUYU285UP&dib=eyJ2IjoiMSJ9.mIoga76smROAydIHua8E2PtxrW_TjPSZXlHzNFa_Y_N9QhetR-GGvsHlGHfexpZP0Aw-kRoOeKri8FteYvapmZL13t1agDOzVon_ggJKlE--55u7SJUnQgWcjc1ZdqfXKPivbJeu8dQX5_x13lpXLZcgr6hOSuXcE8D0wniByqL-rQ4G1zzWu-647Zk26oSXPitd1oWO_DMYPmNFQj7yLQL_pZq4UDJzmBn18VLaSoL1bpP9AF39XPvoPJjpf4cSVkkS5bD8MpgpxHnWA5gWryFxL9BiTBl9n7WRtZaaSac.JR9SNXwixyXccGLXNcSU_z3eExlBpJ1lXQdE2rLE9Zo&dib_tag=se&keywords=Acrylic%2BDesktop%2BOrganizer&qid=1774669908&sprefix=daily%2Bessentials%2Bunder%2B20%2Caps%2C776&sr=8-2&th=1', '{"source": "csv-seed", "keyword": "Acrylic Desktop Organizer", "currency": "USD", "priceText": "12.99元", "priceAmountUsd": 12.99, "detailText": "Product information\nItem details\nBrand Name HBlife\nNumber of Items 1\nManufacturer HBlife\nBest Sellers Rank \n#32,800 in Office Products (See Top 100 in Office Products)\n#238 in Supply Organizers\n#308 in Office Storage Supplies\nASIN B0FS1DQF7G\nCustomer Reviews 4.7 4.7 out of 5 stars   (11)\n4.7 out of 5 stars\nMaterials & Care\nMaterial Type Acrylic\nFinish Types Polished\nFeatures & Specs\nSpecific Uses For Product Stationery\nOther Special Features of the Product Compact\nMounting Type Tabletop Mount\nNumber of Compartments 6\nMeasurements\nItem Dimensions D x W x H 4.6\"D x 8.3\"W x 3.5\"H\nItem Weight 12.32 ounces\nUnit Count 1.0 Count\nStyle\nColor Clear"}'::jsonb),
    ('seed-sr-acrylic-desktop-organizer', 'AMAZON', 'amz-acrylic-02', 2, '4 Tier Acrylic Perfume Organizer Stand, Clear Display Risers for Perfume, Cologne, Cosmetic, Skincare, Funko POP, Dessert Display, Cupcake Stand Holder', 6.63, 'https://m.media-amazon.com/images/I/51nUjXuuH6L._AC_SX679_.jpg', 'https://www.amazon.com/HENABLE-Perfume-Organizer-Acrylic-Display/dp/B0D92G5VYJ/ref=sr_1_3?crid=10II3W4P1Y52Q&dib=eyJ2IjoiMSJ9.mIoga76smROAydIHua8E2PtxrW_TjPSZXlHzNFa_Y_N9QhetR-GGvsHlGHfexpZP0Aw-kRoOeKri8FteYvapmcoOM2HgRhRc0JzDfKNcD_Px7LG9YFznaN1z9bfmd8jsKPivbJeu8dQX5_x13lpXLZcgr6hOSuXcE8D0wniByqL-rQ4G1zzWu-647Zk26oSXPitd1oWO_DMYPmNFQj7yLQL_pZq4UDJzmBn18VLaSoJWlQ-zV7F88Fu-JoYZ3woyhpMahi4irebniuePiWPSRCFxL9BiTBl9n7WRtZaaSac.RMoE5quOv7BAwqCJC7ctkmRU0fwF9guCsGAFRA7JmWM&dib_tag=se&keywords=Acrylic+Desktop+Organizer&qid=1774673545&sprefix=acrylic+desktop+organizer%2Caps%2C610&sr=8-3', '{"source": "csv-seed", "keyword": "Acrylic Desktop Organizer", "currency": "USD", "priceText": "6.63元", "priceAmountUsd": 6.63, "detailText": "Product information\nItem details\nBrand Name HENABLE\nManufacturer HENABLE\nBest Sellers Rank \n#2,183 in Home & Kitchen (See Top 100 in Home & Kitchen)\n#2 in Display Risers\nASIN B0D92G5VYJ\nCustomer Reviews 4.6 4.6 out of 5 stars   (1,954)\n4.6 out of 5 stars\nFeatures & Specs\nBase Type Leg\nNumber of Levels 4\nStyle\nColor Transparency\nItem Shape stand-4-11IN\nMaterials & Care\nMaterial Acrylic\nFinish Type Unfinished\nMeasurements\nItem Weight 14.4 Ounces\nItem Dimensions 11.6 x 10.5 x 5.7 inches\nAdditional details\nRequired Assembly Yes"}'::jsonb),
    ('seed-sr-acrylic-desktop-organizer', 'AMAZON', 'amz-acrylic-03', 3, 'YKLSLH 4 Trays Paper Organizer Letter Tray - Acrylic Desk File Organizer, Stackable Clear Paper Holder Sorter Office Organizer for Letter/A4, Office File Ipad Books Notes Etc', 20.99, 'https://m.media-amazon.com/images/I/716q1WXEf0L._AC_SX679_.jpg', 'https://www.amazon.com/YKLSLH-Trays-Paper-Organizer-Letter/dp/B0FCSBGGQL/ref=sr_1_8?crid=10II3W4P1Y52Q&dib=eyJ2IjoiMSJ9.mIoga76smROAydIHua8E2PtxrW_TjPSZXlHzNFa_Y_N9QhetR-GGvsHlGHfexpZP0Aw-kRoOeKri8FteYvapmcoOM2HgRhRc0JzDfKNcD_Px7LG9YFznaN1z9bfmd8jsKPivbJeu8dQX5_x13lpXLZcgr6hOSuXcE8D0wniByqL-rQ4G1zzWu-647Zk26oSXPitd1oWO_DMYPmNFQj7yLQL_pZq4UDJzmBn18VLaSoJWlQ-zV7F88Fu-JoYZ3woyhpMahi4irebniuePiWPSRCFxL9BiTBl9n7WRtZaaSac.RMoE5quOv7BAwqCJC7ctkmRU0fwF9guCsGAFRA7JmWM&dib_tag=se&keywords=Acrylic%2BDesktop%2BOrganizer&qid=1774673545&sprefix=acrylic%2Bdesktop%2Borganizer%2Caps%2C610&sr=8-8&th=1', '{"source": "csv-seed", "keyword": "Acrylic Desktop Organizer", "currency": "USD", "priceText": "20.99元", "priceAmountUsd": 20.99, "detailText": "Product information\nItem details\nBrand Name YKLSLH\nTray Type Bathroom Tray, Letter Tray, Vanity Tray\nRecommended Uses For Product Letter, Paper, Document\nModel Number 2409\nManufacturer YKLSLH\nItem Type Name letter tray\nUnit Count 4.0 Count\nBest Sellers Rank \n#1,570 in Office Products (See Top 100 in Office Products)\n#6 in Letter Trays & Stacking Supports\nASIN B0FCSBGGQL\nCustomer Reviews 4.6 4.6 out of 5 stars   (150)\n4.6 out of 5 stars\nAdditional details\nSpecial Feature Durable clear acrylic file organizer with stackable design — no assembly needed, easy to clean, and perfect for A4 document storage.\nStyle\nColor 4 Tier-horizontal\nShape Rectangular\nStyle Name Modern\nPattern Solid\nMaterials & Care\nMaterial Type Plastic\nFinish Types Polished\nProduct Care Instructions Wipe with Damp Cloth, Not Dishwasher Safe\nIs Dishwasher Safe No\nMeasurements\nItem Dimensions L x W x H 12.99\"L x 9.45\"W x 10.63\"H\nNumber of Items 1\nItem Weight 1.91 Kilograms"}'::jsonb),
    ('seed-sr-acrylic-desktop-organizer', 'AMAZON', 'amz-acrylic-04', 4, 'Vtopmart 12.1''''W Clear Stackable Storage Drawers,2 Pack Acrylic Plastic Organizers Bins for Makeup Palettes, Cosmetics, and Beauty Supplies,Ideal for Vanity, Bathroom,Cabinet,Desk Organization', 23.74, 'https://m.media-amazon.com/images/I/71Y0CcVz1PL._AC_SX679_PIbundle-2,TopRight,0,0_SH20_.jpg', NULL, '{"source": "csv-seed", "keyword": "Acrylic Desktop Organizer", "currency": "USD", "priceText": "23.74元", "priceAmountUsd": 23.74, "detailText": "Product information\nItem details\nBrand Name Vtopmart\nItem Type Name storage drawers\nIncluded Components Silicone rubber feet\nManufacturer Vtopmart\nUnit Count 2.0 Count\nBest Sellers Rank \n#2,829 in Beauty & Personal Care (See Top 100 in Beauty & Personal Care)\n#2 in Cosmetic Display Cases\nASIN B0CDXLYN2W\nCustomer Reviews 4.8 4.8 out of 5 stars   (4,063)\n4.8 out of 5 stars\nMeasurements\nItem Dimensions D x W x H 7.5\"D x 12.1\"W x 3.5\"H\nMaterials & Care\nMaterial Type Plastic\nFrame Material Plastic\nAdditional details\nColor clear\nNumber of Drawers 2\nUser guide\nRequired Assembly No"}'::jsonb),
    ('seed-sr-acrylic-desktop-organizer', 'AMAZON', 'amz-acrylic-05', 5, 'HSYMQ Acrylic Pen Holder 4 Compartments Clear Pencil Holder Organizer Makeup Brush Holder', 8.99, 'https://m.media-amazon.com/images/I/417K27Vf07L._AC_SX679_.jpg', NULL, '{"source": "csv-seed", "keyword": "Acrylic Desktop Organizer", "currency": "USD", "priceText": "8.99元", "priceAmountUsd": 8.99, "detailText": "Product information\nItem details\nBrand Name HSYMQ\nNumber of Items 1\nManufacturer HSYMQ\nModel Number H083\nIncluded Components 4\nItem Type Name Acrylic Pen Holder 4 Compartments Clear Pencil Holder Organizer Makeup Brush Holder\nBest Sellers Rank \n#1,786 in Office Products (See Top 100 in Office Products)\n#10 in Pencil Holders & Pen Holders\nASIN B0B74S6P6V\nCustomer Reviews 4.8 4.8 out of 5 stars   (1,253)\n4.8 out of 5 stars\nStyle\nColor Transparent\nFeatures & Specs\nSpecific Uses For Product Stationery\nOther Special Features of the Product Portable\nMounting Type Tabletop Mount\nNumber of Compartments 4\nMeasurements\nItem Dimensions D x W x H 2.66\"D x 7.89\"W x 3.54\"H\nItem Weight 0.28 Kilograms\nUnit Count 1.0 Count\nMaterials & Care\nMaterial Type Acrylic\nFinish Types Acrylic"}'::jsonb),
    ('seed-sr-acrylic-desktop-organizer', 'AMAZON', 'amz-acrylic-06', 6, '2 Pack Clear Acrylic Pencil Pen Holder Cup,Desk Accessories Holder,Makeup Brush Storage Organizer,Modern Design Desktop Stationery Organizer for Office School Home Supplies,2.6x 2.6x 4 inches', 6.99, 'https://m.media-amazon.com/images/I/61tIJ5sTMZL._AC_SX679_PIbundle-2,TopRight,0,0_SH20_.jpg', 'https://www.amazon.com/Acrylic-Accessories-Organizer-Stationery-Supplies/dp/B07YJHJ79X/ref=sr_1_10?crid=2KMXEXAUZ5YAT&dib=eyJ2IjoiMSJ9.mIoga76smROAydIHua8E2PtxrW_TjPSZXlHzNFa_Y_N9QhetR-GGvsHlGHfexpZP0Aw-kRoOeKri8FteYvapmcoOM2HgRhRc0JzDfKNcD_Px7LG9YFznaN1z9bfmd8jsKPivbJeu8dQX5_x13lpXLZcgr6hOSuXcE8D0wniByqL-rQ4G1zzWu-647Zk26oSXPitd1oWO_DMYPmNFQj7yLQL_pZq4UDJzmBn18VLaSoJWlQ-zV7F88Fu-JoYZ3woyhpMahi4irebniuePiWPSRCFxL9BiTBl9n7WRtZaaSac.RMoE5quOv7BAwqCJC7ctkmRU0fwF9guCsGAFRA7JmWM&dib_tag=se&keywords=Acrylic%2BDesktop%2BOrganizer&qid=1774676775&sprefix=acrylic%2Bdesktop%2Borganizer%2Caps%2C529&sr=8-10&th=1', '{"source": "csv-seed", "keyword": "Acrylic Desktop Organizer", "currency": "USD", "priceText": "6.99元", "priceAmountUsd": 6.99, "detailText": "Product information\nMeasurements\nItem Dimensions L x W x H 2.6\"L x 2.6\"W x 4\"H\nItem Weight 0.37 Pounds\nUnit Count 2 Count\nCapacity 36 Cubic Inches\nStyle\nColor Clear\nFinish Types Polished\nAdditional details\nMaterial Type Acrylic\nItem details\nBrand Name Cerpourt\nManufacturer Cerpourt\nUPC 708818157649\nBest Sellers Rank \n#1,627 in Office Products (See Top 100 in Office Products)\n#9 in Pencil Holders & Pen Holders\nASIN B07YJHJ79X\nCustomer Reviews 4.7 4.7 out of 5 stars   (4,039)\n4.7 out of 5 stars"}'::jsonb),
    ('seed-sr-acrylic-desktop-organizer', 'AMAZON', 'amz-acrylic-07', 7, 'Large Acrylic Display Risers, Perfume Stand Organizer, Clear Acrylic Shelf Risers for Display Stands for Food, Tabletop Use, Amiibo Funko POP Figure, 3 Tier Clear Cupcake Stand Risers', 6.95, 'https://m.media-amazon.com/images/I/517NSsDc6WL._AC_SX679_.jpg', 'https://www.amazon.com/HENABLE-Acrylic-Display-Organizer-Tabletop/dp/B0C6YDGQPC/ref=sr_1_9?crid=2KMXEXAUZ5YAT&dib=eyJ2IjoiMSJ9.mIoga76smROAydIHua8E2PtxrW_TjPSZXlHzNFa_Y_N9QhetR-GGvsHlGHfexpZP0Aw-kRoOeKri8FteYvapmcoOM2HgRhRc0JzDfKNcD_Px7LG9YFznaN1z9bfmd8jsKPivbJeu8dQX5_x13lpXLZcgr6hOSuXcE8D0wniByqL-rQ4G1zzWu-647Zk26oSXPitd1oWO_DMYPmNFQj7yLQL_pZq4UDJzmBn18VLaSoJWlQ-zV7F88Fu-JoYZ3woyhpMahi4irebniuePiWPSRCFxL9BiTBl9n7WRtZaaSac.RMoE5quOv7BAwqCJC7ctkmRU0fwF9guCsGAFRA7JmWM&dib_tag=se&keywords=Acrylic%2BDesktop%2BOrganizer&qid=1774676775&sprefix=acrylic%2Bdesktop%2Borganizer%2Caps%2C529&sr=8-9&th=1', '{"source": "csv-seed", "keyword": "Acrylic Desktop Organizer", "currency": "USD", "priceText": "6.95元", "priceAmountUsd": 6.95, "detailText": "Product information\nItem details\nBrand Name HENABLE\nBest Sellers Rank \n#3,899 in Home & Kitchen (See Top 100 in Home & Kitchen)\n#5 in Display Risers\nASIN B0C6YDGQPC\nCustomer Reviews 4.4 4.4 out of 5 stars   (4,422)\n4.4 out of 5 stars\nMeasurements\nItem Weight 0.35 Kilograms\nItem Dimensions 8 x 5.7 x 11.6 inches\nAdditional details\nRequired Assembly Yes\nStyle\nColor Clear\nItem Shape 1PACK Large 11.6 IN\nMaterials & Care\nMaterial Acrylic\nFinish Type Polished\nFeatures & Specs\nBase Type Acrylic\nNumber of Levels 3"}'::jsonb),
    ('seed-sr-acrylic-desktop-organizer', 'AMAZON', 'amz-acrylic-08', 8, 'Vtopmart 3 Tier Clear Makeup Organizer with Drawer, Cosmetic Storage for Dresser Countertop and Bathroom Vanity, Beauty Holder for Lipstick Brush Skincare', 17.98, 'https://m.media-amazon.com/images/I/81X2Q3HmAvL._AC_SX679_PIbundle-3,TopRight,0,0_SH20_.jpg', 'https://www.amazon.com/Vtopmart-Organizer-Cosmetic-Countertop-Bathroom/dp/B0D3KNHBZV/ref=sr_1_13?crid=2KMXEXAUZ5YAT&dib=eyJ2IjoiMSJ9.mIoga76smROAydIHua8E2PtxrW_TjPSZXlHzNFa_Y_N9QhetR-GGvsHlGHfexpZP0Aw-kRoOeKri8FteYvapmcoOM2HgRhRc0JzDfKNcD_Px7LG9YFznaN1z9bfmd8jsKPivbJeu8dQX5_x13lpXLZcgr6hOSuXcE8D0wniByqL-rQ4G1zzWu-647Zk26oSXPitd1oWO_DMYPmNFQj7yLQL_pZq4UDJzmBn18VLaSoJWlQ-zV7F88Fu-JoYZ3woyhpMahi4irebniuePiWPSRCFxL9BiTBl9n7WRtZaaSac.RMoE5quOv7BAwqCJC7ctkmRU0fwF9guCsGAFRA7JmWM&dib_tag=se&keywords=Acrylic%2BDesktop%2BOrganizer&qid=1774676775&sprefix=acrylic%2Bdesktop%2Borganizer%2Caps%2C529&sr=8-13&th=1', '{"source": "csv-seed", "keyword": "Acrylic Desktop Organizer", "currency": "USD", "priceText": "17.98元", "priceAmountUsd": 17.98, "detailText": "Product information\nItem details\nBrand Name Vtopmart\nManufacturer Vtopmart\nUnit Count 3.0 Count\nBest Sellers Rank \n#8,251 in Beauty & Personal Care (See Top 100 in Beauty & Personal Care)\n#9 in Cosmetic Display Cases\nASIN B0D3KNHBZV\nCustomer Reviews 4.8 4.8 out of 5 stars   (1,890)\n4.8 out of 5 stars\nUser guide\nRequired Assembly No\nMeasurements\nItem Dimensions D x W x H 9.4\"D x 6.9\"W x 9\"H\nItem Weight 1.63 Kilograms\nMaterials & Care\nMaterial Type Plastic\nFrame Material Plastic\nAdditional details\nColor Clear\nNumber of Drawers 2"}'::jsonb),
    ('seed-sr-acrylic-desktop-organizer', 'AMAZON', 'amz-acrylic-09', 9, '4 Tier Perfume Organizer Stand, Acrylic Risers Display Stands, Display Risers, Perfume Shelf Dessert Display for Party, Figure Stand Funko POP Shelves, Black Cupcake Stand Holder', 8.06, 'https://m.media-amazon.com/images/I/51Gljm6v2eL._AC_SX679_.jpg', 'https://www.amazon.com/HENABLE-Perfume-Organizer-Acrylic-Display/dp/B0D92H8MGX/ref=sr_1_18?crid=2KMXEXAUZ5YAT&dib=eyJ2IjoiMSJ9.mIoga76smROAydIHua8E2PtxrW_TjPSZXlHzNFa_Y_N9QhetR-GGvsHlGHfexpZP0Aw-kRoOeKri8FteYvapmcoOM2HgRhRc0JzDfKNcD_Px7LG9YFznaN1z9bfmd8jsKPivbJeu8dQX5_x13lpXLZcgr6hOSuXcE8D0wniByqL-rQ4G1zzWu-647Zk26oSXPitd1oWO_DMYPmNFQj7yLQL_pZq4UDJzmBn18VLaSoJWlQ-zV7F88Fu-JoYZ3woyhpMahi4irebniuePiWPSRCFxL9BiTBl9n7WRtZaaSac.RMoE5quOv7BAwqCJC7ctkmRU0fwF9guCsGAFRA7JmWM&dib_tag=se&keywords=Acrylic%2BDesktop%2BOrganizer&qid=1774676775&sprefix=acrylic%2Bdesktop%2Borganizer%2Caps%2C529&sr=8-18&th=1', '{"source": "csv-seed", "keyword": "Acrylic Desktop Organizer", "currency": "USD", "priceText": "8.06元", "priceAmountUsd": 8.06, "detailText": "Product information\nItem details\nBrand Name HENABLE\nManufacturer HENABLE\nBest Sellers Rank \n#9,536 in Home & Kitchen (See Top 100 in Home & Kitchen)\n#10 in Display Risers\nASIN B0D92H8MGX\nCustomer Reviews 4.5 4.5 out of 5 stars   (1,440)\n4.5 out of 5 stars\nFeatures & Specs\nNumber of Levels 4\nStyle\nColor black\nItem Shape stand-4-11IN-black\nMaterials & Care\nMaterial Acrylic\nMeasurements\nItem Weight 0.42 Kilograms\nItem Dimensions 11.6 x 10.3 x 5.8 inches\nAdditional details\nRequired Assembly Yes"}'::jsonb)
ON CONFLICT (search_run_id, platform, external_item_id) DO UPDATE SET
    rank_no = EXCLUDED.rank_no,
    title = EXCLUDED.title,
    price = EXCLUDED.price,
    image = EXCLUDED.image,
    link = EXCLUDED.link,
    raw_jsonb = EXCLUDED.raw_jsonb;

INSERT INTO gv_platform_item (
    platform, external_item_id, title, price, rating, reviews, image, link, attributes_jsonb, created_at, updated_at
) VALUES
    ('TAOBAO', 'cn-1688-acrylic-01', '亚马逊爆款亚克力磁吸笔筒冰箱防滑收纳器家居亚克力记号笔收纳盒', 15.00, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01aNtfVb1n0Q3httTK3_!!2211080875027-0-cib.jpg', 'https://detail.1688.com/offer/892035934044.html?spm=a26352.b28411319/2508.0.0', '{"seedTag": "csv-seed", "sourceMarketplace": "1688", "shippingText": "5元", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, NOW(), NOW()),
    ('TAOBAO', 'cn-1688-acrylic-02', '亚克力壁挂式干擦记号笔白板用品支架铅笔橡皮擦收纳盒工艺品批发', 9.98, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01OyVnep1nOEkPH9zqS_!!2212646715079-0-cib.jpg', 'https://detail.1688.com/offer/1034364222648.html?spm=a26352.b28411319/2508.0.0', '{"seedTag": "csv-seed", "sourceMarketplace": "1688", "shippingText": "10元", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, NOW(), NOW()),
    ('TAOBAO', 'cn-1688-acrylic-03', '亚克力磁吸笔筒冰箱防滑收纳器家居亚克力记号笔收纳盒办公收纳架', 15.50, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01bzXp2F2Lc9ijVGaB8_!!2220876419712-0-cib.jpg', NULL, '{"seedTag": "csv-seed", "sourceMarketplace": "1688", "shippingText": "8元", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, NOW(), NOW()),
    ('TAOBAO', 'cn-1688-acrylic-04', '跨境透明亚克力手办盲盒收纳展示架动漫汽车模型香水多层', 3.20, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01nvsyih1tE3cT0G29T_!!3397665869-0-cib.jpg', 'https://detail.1688.com/offer/1000829449552.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=400d0771583a4f17a670ca314e5cda05&sessionid=197563a734474798aa07e6813acdee9b', '{"seedTag": "csv-seed", "sourceMarketplace": "1688", "shippingText": "0元", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, NOW(), NOW()),
    ('TAOBAO', 'cn-1688-acrylic-05', '亚克力展示架手办桌面收纳架有机玻璃多层阶梯产品置物架批发', 5.40, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01vmER7T1yTh3NHlekz_!!945926580-0-cib.jpg', 'https://detail.1688.com/offer/777808157533.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=a76061f5c1cb427b94684083ebe7e41a&sessionid=a4ab613ada4c5ba3e0735437c6a05632', '{"seedTag": "csv-seed", "sourceMarketplace": "1688", "shippingText": "6元", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, NOW(), NOW()),
    ('TAOBAO', 'cn-1688-acrylic-06', '展示立管香水收纳架透明杯形蛋糕支架搁板立管派对甜点架装饰整理', 5.90, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01ymAqg328geWVPwa0l_!!2218547037962-0-cib.jpg', 'https://detail.1688.com/offer/909794074270.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=f67241917f174dd7b59734ad103a8d0d&sessionid=f684a2ea085382a36a67d4c60d2b4f1c', '{"seedTag": "csv-seed", "sourceMarketplace": "1688", "shippingText": "6元", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, NOW(), NOW()),
    ('TAOBAO', 'cn-1688-acrylic-07', '桌面收纳盒可叠加A4纸文件办公抽屉置物盒学生文具用品桌面置物架', 5.00, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01gUhTih2EeGCVJyiGO_!!2215811618769-0-cib.jpg', 'https://detail.1688.com/offer/989497721043.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=de2f21e6ac9147c1b16acd5ea547c931&sessionid=69bc9a5112d4d4c028fbf4921b56c9cd', '{"seedTag": "csv-seed", "sourceMarketplace": "1688", "shippingText": "2.8元", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, NOW(), NOW()),
    ('TAOBAO', 'cn-1688-acrylic-08', '批发办公室桌面文件收纳文件架可叠加多层置物架学生整理老师专用', 10.60, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01eCyjRc1kBSViIXGX3_!!1007574645-0-cib.jpg', 'https://detail.1688.com/offer/766110291276.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=194674d5268142c7b1c37dde66411811&sessionid=163e508d1e8d5123dd27fa65b8edc0f9', '{"seedTag": "csv-seed", "sourceMarketplace": "1688", "shippingText": "4元", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, NOW(), NOW()),
    ('TAOBAO', 'cn-1688-acrylic-09', '桌面文件架A4纸办公室多层收纳盒亚克力书架置物架资料栏透明叠', 4.90, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01YY5cay1K3VtDcn1tz_!!2213293631108-0-cib.jpg', 'https://detail.1688.com/offer/912180445727.html?spm=a26352.b28411319/2508.0.0', '{"seedTag": "csv-seed", "sourceMarketplace": "1688", "shippingText": "3元", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, NOW(), NOW())
ON CONFLICT (platform, external_item_id) DO UPDATE SET
    title = EXCLUDED.title,
    price = EXCLUDED.price,
    rating = EXCLUDED.rating,
    reviews = EXCLUDED.reviews,
    image = EXCLUDED.image,
    link = EXCLUDED.link,
    attributes_jsonb = EXCLUDED.attributes_jsonb,
    updated_at = NOW();

INSERT INTO gv_platform_item_snapshot (
    snapshot_id, platform, external_item_id, query_text, title, price, rating, reviews, image, link, raw_jsonb, created_at
) VALUES
    ('seed-snap-cn-1688-acrylic-01', 'TAOBAO', 'cn-1688-acrylic-01', '亚克力透明收纳架', '亚马逊爆款亚克力磁吸笔筒冰箱防滑收纳器家居亚克力记号笔收纳盒', 15.00, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01aNtfVb1n0Q3httTK3_!!2211080875027-0-cib.jpg', 'https://detail.1688.com/offer/892035934044.html?spm=a26352.b28411319/2508.0.0', '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "双格17元 单格15元", "shippingText": "5元", "detailText": "商品属性\n品牌 \n其他\n主要下游平台 \nwish,抖音,亚马逊,独立站,速卖通,LAZADA,ebay\n主要销售地区 \n东南亚,东北亚,拉丁美洲,非洲,欧洲,中东,中国,其他,南美,北美\n是否跨境出口专供货源 \n是\n产品特性 \n易于清洁\n原产国/地区 \n中国金华\n适用范围 \n家居装饰\n产品类型 \n亚克力盒子\n有可授权的自有品牌 \n否\n包装信息\n商品件重尺\n规格 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n双格 15 9 5 675 200\n单格 15 9 5 675 120", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, NOW()),
    ('seed-snap-cn-1688-acrylic-02', 'TAOBAO', 'cn-1688-acrylic-02', '亚克力透明收纳架', '亚克力壁挂式干擦记号笔白板用品支架铅笔橡皮擦收纳盒工艺品批发', 9.98, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01OyVnep1nOEkPH9zqS_!!2212646715079-0-cib.jpg', 'https://detail.1688.com/offer/1034364222648.html?spm=a26352.b28411319/2508.0.0', '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "9.98元", "shippingText": "10元", "detailText": "商品属性\n材质 \n亚克力\n形状 \n立体形\n加印LOGO \n可以\n品牌 \n恒原\n货号 \nHY-YKL002\n型号 \nHY-YKL002\n规格 \n磁吸笔筒收纳盒\n适用送礼场合 \n婚庆,生日,满月,旅游纪念,毕业,乔迁,派对聚会,探病慰问,其他\n加工定制 \n是\n尺寸 \n/（mm）\n是否进口 \n是\n颜色 \n透明色\n主要下游平台 \nebay,亚马逊,wish,速卖通,独立站,LAZADA,其他\n主要销售地区 \n非洲,欧洲,南美,东南亚,北美,东北亚,中东,其他\n有可授权的自有品牌 \n否\n是否跨境出口专供货源 \n是\n是否属于礼品 \n是，个人礼品\n适用送礼关系 \n晚辈,情侣,夫妻,同事,朋友,长辈,孩子,同学,恩师,其他\n适用节日 \n圣诞节,情人节,春节,父亲节,教师节,七夕,万圣节,复活节,国庆节,儿童节,妇女节\n是否IP授权 \n否\n是否专利货源 \n否\n包装信息\n商品件重尺\n规格 颜色 重量(g)\n磁吸笔筒收纳盒 透明色 200", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, NOW()),
    ('seed-snap-cn-1688-acrylic-03', 'TAOBAO', 'cn-1688-acrylic-03', '亚克力透明收纳架', '亚克力磁吸笔筒冰箱防滑收纳器家居亚克力记号笔收纳盒办公收纳架', 15.50, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01bzXp2F2Lc9ijVGaB8_!!2220876419712-0-cib.jpg', NULL, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "15.5元", "shippingText": "8元", "detailText": "商品属性\n材质 \n亚克力\n品牌 \n睿诚\n货号 \n笔架\n规格 \n咨询\n型号 \n笔架\n颜色 \n透明\n加工定制 \n是\n是否进口 \n否\n尺寸 \n5.9英寸*1.9英寸*3.5英寸\n主要下游平台 \n其他\n主要销售地区 \n其他\n是否跨境出口专供货源 \n是\n是否IP授权 \n否\n包装信息\n商品件重尺\n尺寸 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n5.9英寸*1.9英寸*3.5英寸 14.99 4.83 8.89 643.651 320", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, NOW()),
    ('seed-snap-cn-1688-acrylic-04', 'TAOBAO', 'cn-1688-acrylic-04', '亚克力透明收纳架', '跨境透明亚克力手办盲盒收纳展示架动漫汽车模型香水多层', 3.20, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01nvsyih1tE3cT0G29T_!!3397665869-0-cib.jpg', 'https://detail.1688.com/offer/1000829449552.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=400d0771583a4f17a670ca314e5cda05&sessionid=197563a734474798aa07e6813acdee9b', '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "20cm单层架（3mm层宽8cm）3.2元\n20cm两层架（3mm层宽8cm）5.9元\n20cm三层架（3mm层宽8cm）9.5元\n20cm四层架（3mm层宽8cm）11.7元\n20cm五层架（3mm层宽8cm）23元\n30cm一层架（3mm层宽8cm）4.1元", "shippingText": "0元", "detailText": "商品属性\n品牌 \n上海响民\n产品特性 \n高透明度\n适用范围 \n桌面摆件收纳\n产品类型 \n亚克力阶梯展示架\n产地 \n江苏\n包装信息\n商品件重尺\n规格 重量(g)\n20cm单层架（3mm层宽8cm） 350\n20cm两层架（3mm层宽8cm） 350\n20cm三层架（3mm层宽8cm） 350\n20cm四层架（3mm层宽8cm） 350\n20cm五层架（3mm层宽8cm） 350\n30cm一层架（3mm层宽8cm） 350\n30cm两层架（3mm层宽8cm） 350\n30cm三层架（3mm层宽8cm） 350\n30cm四层架（3mm层宽8cm） 350\n30cm五层架（3mm层宽8cm） 350\n40cm一层架（3mm层宽8cm） 350\n40cm两层架（3mm层宽8cm） 350\n40cm三层架（3mm层宽8cm） 350\n40cm四层架（3mm层宽8cm） 350\n40cm五层架（3mm层宽8cm） 350\n30cm三层架（3mm层宽10cm） 350\n30cm四层架（3mm层宽10cm） 350\n40cm三层架（3mm层宽10cm） 350\n40cm四层架（3mm层宽10cm） 350", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, NOW()),
    ('seed-snap-cn-1688-acrylic-05', 'TAOBAO', 'cn-1688-acrylic-05', '亚克力透明收纳架', '亚克力展示架手办桌面收纳架有机玻璃多层阶梯产品置物架批发', 5.40, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01vmER7T1yTh3NHlekz_!!945926580-0-cib.jpg', 'https://detail.1688.com/offer/777808157533.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=a76061f5c1cb427b94684083ebe7e41a&sessionid=a4ab613ada4c5ba3e0735437c6a05632', '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "单层20长（2085cm）5.4元\n单层30cm长（3085cm）6.3元\n两层20长（201410cm）9.9元\n两层30cm长（301410cm）10.8元\n两层40cm长（401410cm）13.05元\n三层20cm长（202015cm）13.5元\n三层30cm长（302015cm）15.3元\n三层40cm长（402015cm）18.45元\n三层50cm长（502015cm）24.3元\n三层60cm长（602015cm）33.3元\n四层20cm长（202620cm）18元\n四层30cm长（302620cm）21.15元\n四层40cm长（402620cm）28.35元\n四层50cm长（502620cm）35.1元\n四层60cm长（602620cm）43.2元\n五层20cm长（203225cm）25.2元\n五层30cm长（303225cm）27.9元\n五层40cm长（403225cm）37.35元\n五层50cm长（503225cm）44.1元\n五层60cm长（603225cm）52.2元", "shippingText": "6元", "detailText": "商品属性\n产地 \n浙江温州\n品名 \n阶梯置物架\n是否进口 \n否\n牌号 \n手办展示架\n类别 \n有机玻璃展示架\n货号 \nyzsbj\n品牌 \nxybp\n是否属于跨境专供商品 \n否\n规格 \n单层20长（20*8*5cm）,单层30cm长（30*8*5cm）,两层20长（20*14*10cm）,两层30cm长（30*14*10cm）,两层40cm长（40*14*10cm）,三层20cm长（20*20*15cm）,三层30cm长（30*20*15cm）,三层40cm长（40*20*15cm）,三层50cm长（50*20*15cm）,三层60cm长（60*20*15cm）,四层20cm长（20*26*20cm）,四层30cm长（30*26*20cm),四层40cm长（40*26*20cm）,四层50cm长（50*26*20cm）,四层60cm长（60*26*20cm),五层20cm长（20*32*25cm）,五层30cm长（30*32*25cm）,五层40cm长（40*32*25cm）,五层50cm长（50*32*25cm）,五层60cm长（60*32*25cm）\n包装信息\n商品件重尺\n重量(g)\n500", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, NOW()),
    ('seed-snap-cn-1688-acrylic-06', 'TAOBAO', 'cn-1688-acrylic-06', '亚克力透明收纳架', '展示立管香水收纳架透明杯形蛋糕支架搁板立管派对甜点架装饰整理', 5.90, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01ymAqg328geWVPwa0l_!!2218547037962-0-cib.jpg', 'https://detail.1688.com/offer/909794074270.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=f67241917f174dd7b59734ad103a8d0d&sessionid=f684a2ea085382a36a67d4c60d2b4f1c', '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "20cm单层架（3mm层宽8cm）5.9元\n20cm两层架（3mm层宽8cm）8.9元\n20cm三层架（3mm层宽8cm）14.9元\n20cm四层架（3mm层宽8cm）17.9元\n30cm单层架（3mm层宽8cm）6.9元\n30cm两层架（3mm层宽8cm）9.9元\n30cm三层架（3mm层宽8cm）15.9元\n30cm四层架（3mm层宽8cm）23.9元\n40cm单层架（3mm层宽8cm）8.9元\n40cm两层架（3mm层宽8cm）12.9元\n40cm三层架（3mm层宽8cm）18.9元\n40cm四层架（3mm层宽8cm）26.9元\n30cm三层架（3mm层宽10cm）17.9元\n30cm四层架（3mm层宽10cm）24.9元\n40cm三层架（3mm层宽10cm）19.9元\n40cm四层架（3mm层宽10cm）26.9元", "shippingText": "6元", "detailText": "商品属性\n材质 \n塑料\n功能 \n免安装\n层数 \n3层\n品牌 \n其他\n款式 \n展示立管\n收纳场景 \n化妆收纳,厨房收纳,桌面收纳,卧室收纳,卫浴收纳,客厅收纳,花架收纳\n安装方式 \n桌面式\n是否进口 \n否\n风格 \n日式\n贸易属性 \n内贸+外贸\n箱装数量 \n咨询客服\n产品上市时间 \n2025\n货号 \n展示立管\n颜色 \n20cm单层架（3mm层宽8cm）,20cm两层架（3mm层宽8cm）,20cm三层架（3mm层宽8cm）,20cm四层架（3mm层宽8cm）,30cm单层架（3mm层宽8cm）,30cm两层架（3mm层宽8cm）,30cm三层架（3mm层宽8cm）,30cm四层架（3mm层宽8cm）,40cm单层架（3mm层宽8cm）,40cm两层架（3mm层宽8cm）,40cm三层架（3mm层宽8cm）,40cm四层架（3mm层宽8cm）,30cm三层架（3mm层宽10cm）,30cm四层架（3mm层宽10cm）,40cm三层架（3mm层宽10cm）,40cm四层架（3mm层宽10cm）\n专利 \n否\n是否跨境出口专供货源 \n否\n包装信息\n商品件重尺\n颜色 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n20cm单层架（3mm层宽8cm） 20 8 5 800 80\n20cm两层架（3mm层宽8cm） 20 8 5 800 154\n20cm三层架（3mm层宽8cm） 20 8 5 800 242\n20cm四层架（3mm层宽8cm） 20 8 5 800 328\n30cm单层架（3mm层宽8cm） 30 8 5 1200 104\n30cm两层架（3mm层宽8cm） 30 8 5 1200 201\n30cm三层架（3mm层宽8cm） 30 8 5 1200 314\n30cm四层架（3mm层宽8cm） 30 8 5 1200 424\n40cm单层架（3mm层宽8cm） 40 8 5 1600 127\n40cm两层架（3mm层宽8cm） 40 8 5 1600 247\n40cm三层架（3mm层宽8cm） 40 8 5 1600 383\n40cm四层架（3mm层宽8cm） 40 8 5 1600 516\n30cm三层架（3mm层宽10cm） 30 10 5 1500 368\n30cm四层架（3mm层宽10cm） 30 10 5 1500 496\n40cm三层架（3mm层宽10cm） 40 10 5 2000 455\n40cm四层架（3mm层宽10cm） 40 10 5 2000 612", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, NOW()),
    ('seed-snap-cn-1688-acrylic-07', 'TAOBAO', 'cn-1688-acrylic-07', '亚克力透明收纳架', '桌面收纳盒可叠加A4纸文件办公抽屉置物盒学生文具用品桌面置物架', 5.00, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01gUhTih2EeGCVJyiGO_!!2215811618769-0-cib.jpg', 'https://detail.1688.com/offer/989497721043.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=de2f21e6ac9147c1b16acd5ea547c931&sessionid=69bc9a5112d4d4c028fbf4921b56c9cd', '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "横款透明1层5元\n横款透明2层10元\n横款透明4层21元\n竖款透明1层5元\n竖款透明2层10元\n竖款透明4层21元", "shippingText": "2.8元", "detailText": "商品属性\n材质 \npet\n功能 \n可叠加\n产品类别 \n桌面收纳盒\n品牌 \n其他\n容量 \n12\n收纳场景 \n客厅,卧室,书房,餐厅,厨房,浴室,桌上,卫生间\n是否进口 \n否\n型号 \n桌面收纳盒\n商品特性 \n可叠加,其他\n适用范围 \n内衣,CD,遥控器,袜子,首饰,杂志,文具,纸巾,化妆品,杂物,耳机线/电线,其他\n风格 \n现代简约\n图案 \n纯色\n规格 \n大号\n产地 \n浙江\n货号 \n桌面收纳盒2\n加工定制 \n否\n加印LOGO \n不可以\n贸易属性 \n内贸+外贸\n箱装数量 \n66个\n产品上市时间 \n2024年5月\n价格段 \n5元以内\n专利 \n否\n颜色 \n横款透明1层,横款透明2层,横款透明4层,竖款透明1层,竖款透明2层,竖款透明4层\n主要下游平台 \nebay,亚马逊,wish,独立站,LAZADA,其他\n主要销售地区 \n非洲,欧洲,南美,东南亚,北美,东北亚,中东\n有可授权的自有品牌 \n否\n是否跨境出口专供货源 \n是\n毛重 \n285\n桌面文件收纳盒抽屉式 \n整理盒\n桌面文件收纳盒立式 \n桌面文件收纳盒抽屉\n开合方式 \n无盖\n收纳盒长方形 \n桌面收纳盒\n包装信息\n商品件重尺\n颜色 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n横款透明1层 32 24.50 7 5488 300\n横款透明2层 32 24.50 7 5488 600\n横款透明4层 32 24.50 7 5488 1200\n竖款透明1层 32 24.50 7 5488 300\n竖款透明2层 32 24.50 7 5488 600\n竖款透明4层 32 24.50 7 5488 1200", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, NOW()),
    ('seed-snap-cn-1688-acrylic-08', 'TAOBAO', 'cn-1688-acrylic-08', '亚克力透明收纳架', '批发办公室桌面文件收纳文件架可叠加多层置物架学生整理老师专用', 10.60, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01eCyjRc1kBSViIXGX3_!!1007574645-0-cib.jpg', 'https://detail.1688.com/offer/766110291276.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=194674d5268142c7b1c37dde66411811&sessionid=163e508d1e8d5123dd27fa65b8edc0f9', '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "白色宽款一层10.6元\n白色宽款二层20.57元\n白色宽款三层30元\n白色宽款四层39.27元\n白色宽款五层49元\n白色宽款六层58.2元\n白色窄款一层10.6元\n白色窄款二层20.57元\n白色窄款三层30元\n白色窄款四层39.27元\n白色窄款五层49元\n白色窄款六层58.2元\n透明宽款一层10.6元\n透明宽款二层20.57元\n透明宽款三层30元\n透明宽款四层39.27元\n透明宽款五层49元\n透明宽款六层58.2元\n透明窄款一层10.6元\n透明窄款二层20.57元\n透明窄款三层30元\n透明窄款四层39.27元\n透明窄款五层49元\n透明窄款六层58.2元", "shippingText": "4元", "detailText": "商品属性\n材质 \nPET\n产品类别 \n文件架\n层级数 \n多层\n尺寸 \n315*245*70（mm）\n品牌 \n翰达\n类型 \n收纳架，置物架，整理架\n货号 \nHD25001\n规格 \n白色宽款一层,白色宽款二层,白色宽款三层,白色宽款四层,白色宽款五层,白色宽款六层,白色窄款一层,白色窄款二层,白色窄款三层,白色窄款四层,白色窄款五层,白色窄款六层,透明宽款一层,透明宽款二层,透明宽款三层,透明宽款四层,透明宽款五层,透明宽款六层,透明窄款一层,透明窄款二层,透明窄款三层,透明窄款四层,透明窄款五层,透明窄款六层\n型号 \nHD25001\n加印LOGO \n可以\n加工定制 \n否\n层数 \n一层\n层间距 \n7（cm）\n是否跨境出口专供货源 \n否\n贸易属性 \n内贸+外贸\n款式 \n桌面款\n颜色 \n白色或透明 可定制颜色\n包装信息\n商品件重尺\n规格 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n白色宽款一层 31.50 24.50 7 5402.250 464\n白色宽款二层 31.50 24.50 14 10804.500 928\n白色宽款三层 31.50 24.50 21 16206.750 1392\n白色宽款四层 31.50 24.50 28 21609 1856\n白色宽款五层 31.50 24.50 35 27011.250 2320\n白色宽款六层 31.50 24.50 42 32413.500 2784\n白色窄款一层 24.50 31.50 14 10804.500 464\n白色窄款二层 24.50 31.50 14 10804.500 928\n白色窄款三层 24.50 31.50 21 16206.750 1392\n白色窄款四层 24.50 31.50 28 21609 1856\n白色窄款五层 24.50 31.50 35 27011.250 2320\n白色窄款六层 31.50 24.50 42 32413.500 2784\n透明宽款一层 31.50 24.50 7 5402.250 445\n透明宽款二层 31.50 24.50 14 10804.500 890\n透明宽款三层 31.50 24.50 21 16206.750 1335\n透明宽款四层 31.50 24.50 28 21609 1780\n透明宽款五层 31.50 24.50 35 27011.250 2225\n透明宽款六层 31.50 24.50 7 5402.250 2670\n透明窄款一层 24.50 31.50 7 5402.250 445\n透明窄款二层 24.50 31.50 14 10804.500 890\n透明窄款三层 24.50 31.50 21 16206.750 1335\n透明窄款四层 24.50 31.50 28 21609 1780\n透明窄款五层 24.50 31.50 35 27011.250 2225\n透明窄款六层 24.50 31.50 42 32413.500 2670", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, NOW()),
    ('seed-snap-cn-1688-acrylic-09', 'TAOBAO', 'cn-1688-acrylic-09', '亚克力透明收纳架', '桌面文件架A4纸办公室多层收纳盒亚克力书架置物架资料栏透明叠', 4.90, NULL, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01YY5cay1K3VtDcn1tz_!!2213293631108-0-cib.jpg', 'https://detail.1688.com/offer/912180445727.html?spm=a26352.b28411319/2508.0.0', '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "横款款1层【加厚高透-强力承重】4.9元\n横款款2层【加厚高透-强力承重】9.8元\n横款款3层【加厚高透-强力承重】15.8元\n横款款4层【加厚高透-强力承重】20.8元\n竖款1层【加厚高透-强力承重】4.9元\n竖款2层【加厚高透-强力承重】9.8元\n竖款3层【加厚高透-强力承重】15.8元\n竖款4层【加厚高透-强力承重】20.8元", "shippingText": "3元", "detailText": "商品属性\n材质 \n塑料\n产品类别 \n文件架\n层级数 \n5层\n尺寸 \n其他（mm）\n品牌 \n华盈家尚\n收容量 \n25\n类型 \n文件栏\n货号 \n250416\n规格 \n横款款1层【加厚高透-强力承重】,横款款2层【加厚高透-强力承重】,横款款3层【加厚高透-强力承重】,横款款4层【加厚高透-强力承重】,竖款1层【加厚高透-强力承重】,竖款2层【加厚高透-强力承重】,竖款3层【加厚高透-强力承重】,竖款4层【加厚高透-强力承重】\n型号 \n1\n加印LOGO \n不可以\n加工定制 \n否\n层数 \n5\n层间距 \n其他（cm）\n是否跨境出口专供货源 \n否\n包装信息\n商品件重尺\n规格 重量(g)\n横款款1层【加厚高透-强力承重】 600\n横款款2层【加厚高透-强力承重】 1200\n横款款3层【加厚高透-强力承重】 2000\n横款款4层【加厚高透-强力承重】 2600\n竖款1层【加厚高透-强力承重】 600\n竖款2层【加厚高透-强力承重】 1200\n竖款3层【加厚高透-强力承重】 2000\n竖款4层【加厚高透-强力承重】 2600", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, NOW())
ON CONFLICT (snapshot_id) DO UPDATE SET
    platform = EXCLUDED.platform,
    external_item_id = EXCLUDED.external_item_id,
    query_text = EXCLUDED.query_text,
    title = EXCLUDED.title,
    price = EXCLUDED.price,
    rating = EXCLUDED.rating,
    reviews = EXCLUDED.reviews,
    image = EXCLUDED.image,
    link = EXCLUDED.link,
    raw_jsonb = EXCLUDED.raw_jsonb,
    created_at = EXCLUDED.created_at;

INSERT INTO product_detail_snapshot (
    product_id, platform, title, price, brand, image, link, description, gallery_json, attributes_json, sku_data_json, raw_data_json, created_at, updated_at
) VALUES
    ('cn-1688-acrylic-01', 'TAOBAO', '亚马逊爆款亚克力磁吸笔筒冰箱防滑收纳器家居亚克力记号笔收纳盒', 15.00, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01aNtfVb1n0Q3httTK3_!!2211080875027-0-cib.jpg', 'https://detail.1688.com/offer/892035934044.html?spm=a26352.b28411319/2508.0.0', '商品属性
品牌 
其他
主要下游平台 
wish,抖音,亚马逊,独立站,速卖通,LAZADA,ebay
主要销售地区 
东南亚,东北亚,拉丁美洲,非洲,欧洲,中东,中国,其他,南美,北美
是否跨境出口专供货源 
是
产品特性 
易于清洁
原产国/地区 
中国金华
适用范围 
家居装饰
产品类型 
亚克力盒子
有可授权的自有品牌 
否
包装信息
商品件重尺
规格 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)
双格 15 9 5 675 200
单格 15 9 5 675 120', '["https://cbu01.alicdn.com/img/ibank/O1CN01aNtfVb1n0Q3httTK3_!!2211080875027-0-cib.jpg"]'::jsonb, '{"fixedKeyword": "亚克力透明收纳架", "sourceMarketplace": "1688", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, '{"priceText": "双格17元 单格15元", "shippingText": "5元", "basePriceCny": 15.0}'::jsonb, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "双格17元 单格15元", "shippingText": "5元", "detailText": "商品属性\n品牌 \n其他\n主要下游平台 \nwish,抖音,亚马逊,独立站,速卖通,LAZADA,ebay\n主要销售地区 \n东南亚,东北亚,拉丁美洲,非洲,欧洲,中东,中国,其他,南美,北美\n是否跨境出口专供货源 \n是\n产品特性 \n易于清洁\n原产国/地区 \n中国金华\n适用范围 \n家居装饰\n产品类型 \n亚克力盒子\n有可授权的自有品牌 \n否\n包装信息\n商品件重尺\n规格 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n双格 15 9 5 675 200\n单格 15 9 5 675 120", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, NOW(), NOW()),
    ('cn-1688-acrylic-02', 'TAOBAO', '亚克力壁挂式干擦记号笔白板用品支架铅笔橡皮擦收纳盒工艺品批发', 9.98, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01OyVnep1nOEkPH9zqS_!!2212646715079-0-cib.jpg', 'https://detail.1688.com/offer/1034364222648.html?spm=a26352.b28411319/2508.0.0', '商品属性
材质 
亚克力
形状 
立体形
加印LOGO 
可以
品牌 
恒原
货号 
HY-YKL002
型号 
HY-YKL002
规格 
磁吸笔筒收纳盒
适用送礼场合 
婚庆,生日,满月,旅游纪念,毕业,乔迁,派对聚会,探病慰问,其他
加工定制 
是
尺寸 
/（mm）
是否进口 
是
颜色 
透明色
主要下游平台 
ebay,亚马逊,wish,速卖通,独立站,LAZADA,其他
主要销售地区 
非洲,欧洲,南美,东南亚,北美,东北亚,中东,其他
有可授权的自有品牌 
否
是否跨境出口专供货源 
是
是否属于礼品 
是，个人礼品
适用送礼关系 
晚辈,情侣,夫妻,同事,朋友,长辈,孩子,同学,恩师,其他
适用节日 
圣诞节,情人节,春节,父亲节,教师节,七夕,万圣节,复活节,国庆节,儿童节,妇女节
是否IP授权 
否
是否专利货源 
否
包装信息
商品件重尺
规格 颜色 重量(g)
磁吸笔筒收纳盒 透明色 200', '["https://cbu01.alicdn.com/img/ibank/O1CN01OyVnep1nOEkPH9zqS_!!2212646715079-0-cib.jpg"]'::jsonb, '{"fixedKeyword": "亚克力透明收纳架", "sourceMarketplace": "1688", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, '{"priceText": "9.98元", "shippingText": "10元", "basePriceCny": 9.98}'::jsonb, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "9.98元", "shippingText": "10元", "detailText": "商品属性\n材质 \n亚克力\n形状 \n立体形\n加印LOGO \n可以\n品牌 \n恒原\n货号 \nHY-YKL002\n型号 \nHY-YKL002\n规格 \n磁吸笔筒收纳盒\n适用送礼场合 \n婚庆,生日,满月,旅游纪念,毕业,乔迁,派对聚会,探病慰问,其他\n加工定制 \n是\n尺寸 \n/（mm）\n是否进口 \n是\n颜色 \n透明色\n主要下游平台 \nebay,亚马逊,wish,速卖通,独立站,LAZADA,其他\n主要销售地区 \n非洲,欧洲,南美,东南亚,北美,东北亚,中东,其他\n有可授权的自有品牌 \n否\n是否跨境出口专供货源 \n是\n是否属于礼品 \n是，个人礼品\n适用送礼关系 \n晚辈,情侣,夫妻,同事,朋友,长辈,孩子,同学,恩师,其他\n适用节日 \n圣诞节,情人节,春节,父亲节,教师节,七夕,万圣节,复活节,国庆节,儿童节,妇女节\n是否IP授权 \n否\n是否专利货源 \n否\n包装信息\n商品件重尺\n规格 颜色 重量(g)\n磁吸笔筒收纳盒 透明色 200", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, NOW(), NOW()),
    ('cn-1688-acrylic-03', 'TAOBAO', '亚克力磁吸笔筒冰箱防滑收纳器家居亚克力记号笔收纳盒办公收纳架', 15.50, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01bzXp2F2Lc9ijVGaB8_!!2220876419712-0-cib.jpg', NULL, '商品属性
材质 
亚克力
品牌 
睿诚
货号 
笔架
规格 
咨询
型号 
笔架
颜色 
透明
加工定制 
是
是否进口 
否
尺寸 
5.9英寸*1.9英寸*3.5英寸
主要下游平台 
其他
主要销售地区 
其他
是否跨境出口专供货源 
是
是否IP授权 
否
包装信息
商品件重尺
尺寸 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)
5.9英寸*1.9英寸*3.5英寸 14.99 4.83 8.89 643.651 320', '["https://cbu01.alicdn.com/img/ibank/O1CN01bzXp2F2Lc9ijVGaB8_!!2220876419712-0-cib.jpg"]'::jsonb, '{"fixedKeyword": "亚克力透明收纳架", "sourceMarketplace": "1688", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, '{"priceText": "15.5元", "shippingText": "8元", "basePriceCny": 15.5}'::jsonb, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "15.5元", "shippingText": "8元", "detailText": "商品属性\n材质 \n亚克力\n品牌 \n睿诚\n货号 \n笔架\n规格 \n咨询\n型号 \n笔架\n颜色 \n透明\n加工定制 \n是\n是否进口 \n否\n尺寸 \n5.9英寸*1.9英寸*3.5英寸\n主要下游平台 \n其他\n主要销售地区 \n其他\n是否跨境出口专供货源 \n是\n是否IP授权 \n否\n包装信息\n商品件重尺\n尺寸 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n5.9英寸*1.9英寸*3.5英寸 14.99 4.83 8.89 643.651 320", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-01"}'::jsonb, NOW(), NOW()),
    ('cn-1688-acrylic-04', 'TAOBAO', '跨境透明亚克力手办盲盒收纳展示架动漫汽车模型香水多层', 3.20, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01nvsyih1tE3cT0G29T_!!3397665869-0-cib.jpg', 'https://detail.1688.com/offer/1000829449552.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=400d0771583a4f17a670ca314e5cda05&sessionid=197563a734474798aa07e6813acdee9b', '商品属性
品牌 
上海响民
产品特性 
高透明度
适用范围 
桌面摆件收纳
产品类型 
亚克力阶梯展示架
产地 
江苏
包装信息
商品件重尺
规格 重量(g)
20cm单层架（3mm层宽8cm） 350
20cm两层架（3mm层宽8cm） 350
20cm三层架（3mm层宽8cm） 350
20cm四层架（3mm层宽8cm） 350
20cm五层架（3mm层宽8cm） 350
30cm一层架（3mm层宽8cm） 350
30cm两层架（3mm层宽8cm） 350
30cm三层架（3mm层宽8cm） 350
30cm四层架（3mm层宽8cm） 350
30cm五层架（3mm层宽8cm） 350
40cm一层架（3mm层宽8cm） 350
40cm两层架（3mm层宽8cm） 350
40cm三层架（3mm层宽8cm） 350
40cm四层架（3mm层宽8cm） 350
40cm五层架（3mm层宽8cm） 350
30cm三层架（3mm层宽10cm） 350
30cm四层架（3mm层宽10cm） 350
40cm三层架（3mm层宽10cm） 350
40cm四层架（3mm层宽10cm） 350', '["https://cbu01.alicdn.com/img/ibank/O1CN01nvsyih1tE3cT0G29T_!!3397665869-0-cib.jpg"]'::jsonb, '{"fixedKeyword": "亚克力透明收纳架", "sourceMarketplace": "1688", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, '{"priceText": "20cm单层架（3mm层宽8cm）3.2元\n20cm两层架（3mm层宽8cm）5.9元\n20cm三层架（3mm层宽8cm）9.5元\n20cm四层架（3mm层宽8cm）11.7元\n20cm五层架（3mm层宽8cm）23元\n30cm一层架（3mm层宽8cm）4.1元", "shippingText": "0元", "basePriceCny": 3.2}'::jsonb, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "20cm单层架（3mm层宽8cm）3.2元\n20cm两层架（3mm层宽8cm）5.9元\n20cm三层架（3mm层宽8cm）9.5元\n20cm四层架（3mm层宽8cm）11.7元\n20cm五层架（3mm层宽8cm）23元\n30cm一层架（3mm层宽8cm）4.1元", "shippingText": "0元", "detailText": "商品属性\n品牌 \n上海响民\n产品特性 \n高透明度\n适用范围 \n桌面摆件收纳\n产品类型 \n亚克力阶梯展示架\n产地 \n江苏\n包装信息\n商品件重尺\n规格 重量(g)\n20cm单层架（3mm层宽8cm） 350\n20cm两层架（3mm层宽8cm） 350\n20cm三层架（3mm层宽8cm） 350\n20cm四层架（3mm层宽8cm） 350\n20cm五层架（3mm层宽8cm） 350\n30cm一层架（3mm层宽8cm） 350\n30cm两层架（3mm层宽8cm） 350\n30cm三层架（3mm层宽8cm） 350\n30cm四层架（3mm层宽8cm） 350\n30cm五层架（3mm层宽8cm） 350\n40cm一层架（3mm层宽8cm） 350\n40cm两层架（3mm层宽8cm） 350\n40cm三层架（3mm层宽8cm） 350\n40cm四层架（3mm层宽8cm） 350\n40cm五层架（3mm层宽8cm） 350\n30cm三层架（3mm层宽10cm） 350\n30cm四层架（3mm层宽10cm） 350\n40cm三层架（3mm层宽10cm） 350\n40cm四层架（3mm层宽10cm） 350", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, NOW(), NOW()),
    ('cn-1688-acrylic-05', 'TAOBAO', '亚克力展示架手办桌面收纳架有机玻璃多层阶梯产品置物架批发', 5.40, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01vmER7T1yTh3NHlekz_!!945926580-0-cib.jpg', 'https://detail.1688.com/offer/777808157533.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=a76061f5c1cb427b94684083ebe7e41a&sessionid=a4ab613ada4c5ba3e0735437c6a05632', '商品属性
产地 
浙江温州
品名 
阶梯置物架
是否进口 
否
牌号 
手办展示架
类别 
有机玻璃展示架
货号 
yzsbj
品牌 
xybp
是否属于跨境专供商品 
否
规格 
单层20长（20*8*5cm）,单层30cm长（30*8*5cm）,两层20长（20*14*10cm）,两层30cm长（30*14*10cm）,两层40cm长（40*14*10cm）,三层20cm长（20*20*15cm）,三层30cm长（30*20*15cm）,三层40cm长（40*20*15cm）,三层50cm长（50*20*15cm）,三层60cm长（60*20*15cm）,四层20cm长（20*26*20cm）,四层30cm长（30*26*20cm),四层40cm长（40*26*20cm）,四层50cm长（50*26*20cm）,四层60cm长（60*26*20cm),五层20cm长（20*32*25cm）,五层30cm长（30*32*25cm）,五层40cm长（40*32*25cm）,五层50cm长（50*32*25cm）,五层60cm长（60*32*25cm）
包装信息
商品件重尺
重量(g)
500', '["https://cbu01.alicdn.com/img/ibank/O1CN01vmER7T1yTh3NHlekz_!!945926580-0-cib.jpg"]'::jsonb, '{"fixedKeyword": "亚克力透明收纳架", "sourceMarketplace": "1688", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, '{"priceText": "单层20长（2085cm）5.4元\n单层30cm长（3085cm）6.3元\n两层20长（201410cm）9.9元\n两层30cm长（301410cm）10.8元\n两层40cm长（401410cm）13.05元\n三层20cm长（202015cm）13.5元\n三层30cm长（302015cm）15.3元\n三层40cm长（402015cm）18.45元\n三层50cm长（502015cm）24.3元\n三层60cm长（602015cm）33.3元\n四层20cm长（202620cm）18元\n四层30cm长（302620cm）21.15元\n四层40cm长（402620cm）28.35元\n四层50cm长（502620cm）35.1元\n四层60cm长（602620cm）43.2元\n五层20cm长（203225cm）25.2元\n五层30cm长（303225cm）27.9元\n五层40cm长（403225cm）37.35元\n五层50cm长（503225cm）44.1元\n五层60cm长（603225cm）52.2元", "shippingText": "6元", "basePriceCny": 5.4}'::jsonb, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "单层20长（2085cm）5.4元\n单层30cm长（3085cm）6.3元\n两层20长（201410cm）9.9元\n两层30cm长（301410cm）10.8元\n两层40cm长（401410cm）13.05元\n三层20cm长（202015cm）13.5元\n三层30cm长（302015cm）15.3元\n三层40cm长（402015cm）18.45元\n三层50cm长（502015cm）24.3元\n三层60cm长（602015cm）33.3元\n四层20cm长（202620cm）18元\n四层30cm长（302620cm）21.15元\n四层40cm长（402620cm）28.35元\n四层50cm长（502620cm）35.1元\n四层60cm长（602620cm）43.2元\n五层20cm长（203225cm）25.2元\n五层30cm长（303225cm）27.9元\n五层40cm长（403225cm）37.35元\n五层50cm长（503225cm）44.1元\n五层60cm长（603225cm）52.2元", "shippingText": "6元", "detailText": "商品属性\n产地 \n浙江温州\n品名 \n阶梯置物架\n是否进口 \n否\n牌号 \n手办展示架\n类别 \n有机玻璃展示架\n货号 \nyzsbj\n品牌 \nxybp\n是否属于跨境专供商品 \n否\n规格 \n单层20长（20*8*5cm）,单层30cm长（30*8*5cm）,两层20长（20*14*10cm）,两层30cm长（30*14*10cm）,两层40cm长（40*14*10cm）,三层20cm长（20*20*15cm）,三层30cm长（30*20*15cm）,三层40cm长（40*20*15cm）,三层50cm长（50*20*15cm）,三层60cm长（60*20*15cm）,四层20cm长（20*26*20cm）,四层30cm长（30*26*20cm),四层40cm长（40*26*20cm）,四层50cm长（50*26*20cm）,四层60cm长（60*26*20cm),五层20cm长（20*32*25cm）,五层30cm长（30*32*25cm）,五层40cm长（40*32*25cm）,五层50cm长（50*32*25cm）,五层60cm长（60*32*25cm）\n包装信息\n商品件重尺\n重量(g)\n500", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, NOW(), NOW()),
    ('cn-1688-acrylic-06', 'TAOBAO', '展示立管香水收纳架透明杯形蛋糕支架搁板立管派对甜点架装饰整理', 5.90, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01ymAqg328geWVPwa0l_!!2218547037962-0-cib.jpg', 'https://detail.1688.com/offer/909794074270.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=f67241917f174dd7b59734ad103a8d0d&sessionid=f684a2ea085382a36a67d4c60d2b4f1c', '商品属性
材质 
塑料
功能 
免安装
层数 
3层
品牌 
其他
款式 
展示立管
收纳场景 
化妆收纳,厨房收纳,桌面收纳,卧室收纳,卫浴收纳,客厅收纳,花架收纳
安装方式 
桌面式
是否进口 
否
风格 
日式
贸易属性 
内贸+外贸
箱装数量 
咨询客服
产品上市时间 
2025
货号 
展示立管
颜色 
20cm单层架（3mm层宽8cm）,20cm两层架（3mm层宽8cm）,20cm三层架（3mm层宽8cm）,20cm四层架（3mm层宽8cm）,30cm单层架（3mm层宽8cm）,30cm两层架（3mm层宽8cm）,30cm三层架（3mm层宽8cm）,30cm四层架（3mm层宽8cm）,40cm单层架（3mm层宽8cm）,40cm两层架（3mm层宽8cm）,40cm三层架（3mm层宽8cm）,40cm四层架（3mm层宽8cm）,30cm三层架（3mm层宽10cm）,30cm四层架（3mm层宽10cm）,40cm三层架（3mm层宽10cm）,40cm四层架（3mm层宽10cm）
专利 
否
是否跨境出口专供货源 
否
包装信息
商品件重尺
颜色 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)
20cm单层架（3mm层宽8cm） 20 8 5 800 80
20cm两层架（3mm层宽8cm） 20 8 5 800 154
20cm三层架（3mm层宽8cm） 20 8 5 800 242
20cm四层架（3mm层宽8cm） 20 8 5 800 328
30cm单层架（3mm层宽8cm） 30 8 5 1200 104
30cm两层架（3mm层宽8cm） 30 8 5 1200 201
30cm三层架（3mm层宽8cm） 30 8 5 1200 314
30cm四层架（3mm层宽8cm） 30 8 5 1200 424
40cm单层架（3mm层宽8cm） 40 8 5 1600 127
40cm两层架（3mm层宽8cm） 40 8 5 1600 247
40cm三层架（3mm层宽8cm） 40 8 5 1600 383
40cm四层架（3mm层宽8cm） 40 8 5 1600 516
30cm三层架（3mm层宽10cm） 30 10 5 1500 368
30cm四层架（3mm层宽10cm） 30 10 5 1500 496
40cm三层架（3mm层宽10cm） 40 10 5 2000 455
40cm四层架（3mm层宽10cm） 40 10 5 2000 612', '["https://cbu01.alicdn.com/img/ibank/O1CN01ymAqg328geWVPwa0l_!!2218547037962-0-cib.jpg"]'::jsonb, '{"fixedKeyword": "亚克力透明收纳架", "sourceMarketplace": "1688", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, '{"priceText": "20cm单层架（3mm层宽8cm）5.9元\n20cm两层架（3mm层宽8cm）8.9元\n20cm三层架（3mm层宽8cm）14.9元\n20cm四层架（3mm层宽8cm）17.9元\n30cm单层架（3mm层宽8cm）6.9元\n30cm两层架（3mm层宽8cm）9.9元\n30cm三层架（3mm层宽8cm）15.9元\n30cm四层架（3mm层宽8cm）23.9元\n40cm单层架（3mm层宽8cm）8.9元\n40cm两层架（3mm层宽8cm）12.9元\n40cm三层架（3mm层宽8cm）18.9元\n40cm四层架（3mm层宽8cm）26.9元\n30cm三层架（3mm层宽10cm）17.9元\n30cm四层架（3mm层宽10cm）24.9元\n40cm三层架（3mm层宽10cm）19.9元\n40cm四层架（3mm层宽10cm）26.9元", "shippingText": "6元", "basePriceCny": 5.9}'::jsonb, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "20cm单层架（3mm层宽8cm）5.9元\n20cm两层架（3mm层宽8cm）8.9元\n20cm三层架（3mm层宽8cm）14.9元\n20cm四层架（3mm层宽8cm）17.9元\n30cm单层架（3mm层宽8cm）6.9元\n30cm两层架（3mm层宽8cm）9.9元\n30cm三层架（3mm层宽8cm）15.9元\n30cm四层架（3mm层宽8cm）23.9元\n40cm单层架（3mm层宽8cm）8.9元\n40cm两层架（3mm层宽8cm）12.9元\n40cm三层架（3mm层宽8cm）18.9元\n40cm四层架（3mm层宽8cm）26.9元\n30cm三层架（3mm层宽10cm）17.9元\n30cm四层架（3mm层宽10cm）24.9元\n40cm三层架（3mm层宽10cm）19.9元\n40cm四层架（3mm层宽10cm）26.9元", "shippingText": "6元", "detailText": "商品属性\n材质 \n塑料\n功能 \n免安装\n层数 \n3层\n品牌 \n其他\n款式 \n展示立管\n收纳场景 \n化妆收纳,厨房收纳,桌面收纳,卧室收纳,卫浴收纳,客厅收纳,花架收纳\n安装方式 \n桌面式\n是否进口 \n否\n风格 \n日式\n贸易属性 \n内贸+外贸\n箱装数量 \n咨询客服\n产品上市时间 \n2025\n货号 \n展示立管\n颜色 \n20cm单层架（3mm层宽8cm）,20cm两层架（3mm层宽8cm）,20cm三层架（3mm层宽8cm）,20cm四层架（3mm层宽8cm）,30cm单层架（3mm层宽8cm）,30cm两层架（3mm层宽8cm）,30cm三层架（3mm层宽8cm）,30cm四层架（3mm层宽8cm）,40cm单层架（3mm层宽8cm）,40cm两层架（3mm层宽8cm）,40cm三层架（3mm层宽8cm）,40cm四层架（3mm层宽8cm）,30cm三层架（3mm层宽10cm）,30cm四层架（3mm层宽10cm）,40cm三层架（3mm层宽10cm）,40cm四层架（3mm层宽10cm）\n专利 \n否\n是否跨境出口专供货源 \n否\n包装信息\n商品件重尺\n颜色 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n20cm单层架（3mm层宽8cm） 20 8 5 800 80\n20cm两层架（3mm层宽8cm） 20 8 5 800 154\n20cm三层架（3mm层宽8cm） 20 8 5 800 242\n20cm四层架（3mm层宽8cm） 20 8 5 800 328\n30cm单层架（3mm层宽8cm） 30 8 5 1200 104\n30cm两层架（3mm层宽8cm） 30 8 5 1200 201\n30cm三层架（3mm层宽8cm） 30 8 5 1200 314\n30cm四层架（3mm层宽8cm） 30 8 5 1200 424\n40cm单层架（3mm层宽8cm） 40 8 5 1600 127\n40cm两层架（3mm层宽8cm） 40 8 5 1600 247\n40cm三层架（3mm层宽8cm） 40 8 5 1600 383\n40cm四层架（3mm层宽8cm） 40 8 5 1600 516\n30cm三层架（3mm层宽10cm） 30 10 5 1500 368\n30cm四层架（3mm层宽10cm） 30 10 5 1500 496\n40cm三层架（3mm层宽10cm） 40 10 5 2000 455\n40cm四层架（3mm层宽10cm） 40 10 5 2000 612", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-02"}'::jsonb, NOW(), NOW()),
    ('cn-1688-acrylic-07', 'TAOBAO', '桌面收纳盒可叠加A4纸文件办公抽屉置物盒学生文具用品桌面置物架', 5.00, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01gUhTih2EeGCVJyiGO_!!2215811618769-0-cib.jpg', 'https://detail.1688.com/offer/989497721043.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=de2f21e6ac9147c1b16acd5ea547c931&sessionid=69bc9a5112d4d4c028fbf4921b56c9cd', '商品属性
材质 
pet
功能 
可叠加
产品类别 
桌面收纳盒
品牌 
其他
容量 
12
收纳场景 
客厅,卧室,书房,餐厅,厨房,浴室,桌上,卫生间
是否进口 
否
型号 
桌面收纳盒
商品特性 
可叠加,其他
适用范围 
内衣,CD,遥控器,袜子,首饰,杂志,文具,纸巾,化妆品,杂物,耳机线/电线,其他
风格 
现代简约
图案 
纯色
规格 
大号
产地 
浙江
货号 
桌面收纳盒2
加工定制 
否
加印LOGO 
不可以
贸易属性 
内贸+外贸
箱装数量 
66个
产品上市时间 
2024年5月
价格段 
5元以内
专利 
否
颜色 
横款透明1层,横款透明2层,横款透明4层,竖款透明1层,竖款透明2层,竖款透明4层
主要下游平台 
ebay,亚马逊,wish,独立站,LAZADA,其他
主要销售地区 
非洲,欧洲,南美,东南亚,北美,东北亚,中东
有可授权的自有品牌 
否
是否跨境出口专供货源 
是
毛重 
285
桌面文件收纳盒抽屉式 
整理盒
桌面文件收纳盒立式 
桌面文件收纳盒抽屉
开合方式 
无盖
收纳盒长方形 
桌面收纳盒
包装信息
商品件重尺
颜色 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)
横款透明1层 32 24.50 7 5488 300
横款透明2层 32 24.50 7 5488 600
横款透明4层 32 24.50 7 5488 1200
竖款透明1层 32 24.50 7 5488 300
竖款透明2层 32 24.50 7 5488 600
竖款透明4层 32 24.50 7 5488 1200', '["https://cbu01.alicdn.com/img/ibank/O1CN01gUhTih2EeGCVJyiGO_!!2215811618769-0-cib.jpg"]'::jsonb, '{"fixedKeyword": "亚克力透明收纳架", "sourceMarketplace": "1688", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, '{"priceText": "横款透明1层5元\n横款透明2层10元\n横款透明4层21元\n竖款透明1层5元\n竖款透明2层10元\n竖款透明4层21元", "shippingText": "2.8元", "basePriceCny": 5.0}'::jsonb, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "横款透明1层5元\n横款透明2层10元\n横款透明4层21元\n竖款透明1层5元\n竖款透明2层10元\n竖款透明4层21元", "shippingText": "2.8元", "detailText": "商品属性\n材质 \npet\n功能 \n可叠加\n产品类别 \n桌面收纳盒\n品牌 \n其他\n容量 \n12\n收纳场景 \n客厅,卧室,书房,餐厅,厨房,浴室,桌上,卫生间\n是否进口 \n否\n型号 \n桌面收纳盒\n商品特性 \n可叠加,其他\n适用范围 \n内衣,CD,遥控器,袜子,首饰,杂志,文具,纸巾,化妆品,杂物,耳机线/电线,其他\n风格 \n现代简约\n图案 \n纯色\n规格 \n大号\n产地 \n浙江\n货号 \n桌面收纳盒2\n加工定制 \n否\n加印LOGO \n不可以\n贸易属性 \n内贸+外贸\n箱装数量 \n66个\n产品上市时间 \n2024年5月\n价格段 \n5元以内\n专利 \n否\n颜色 \n横款透明1层,横款透明2层,横款透明4层,竖款透明1层,竖款透明2层,竖款透明4层\n主要下游平台 \nebay,亚马逊,wish,独立站,LAZADA,其他\n主要销售地区 \n非洲,欧洲,南美,东南亚,北美,东北亚,中东\n有可授权的自有品牌 \n否\n是否跨境出口专供货源 \n是\n毛重 \n285\n桌面文件收纳盒抽屉式 \n整理盒\n桌面文件收纳盒立式 \n桌面文件收纳盒抽屉\n开合方式 \n无盖\n收纳盒长方形 \n桌面收纳盒\n包装信息\n商品件重尺\n颜色 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n横款透明1层 32 24.50 7 5488 300\n横款透明2层 32 24.50 7 5488 600\n横款透明4层 32 24.50 7 5488 1200\n竖款透明1层 32 24.50 7 5488 300\n竖款透明2层 32 24.50 7 5488 600\n竖款透明4层 32 24.50 7 5488 1200", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, NOW(), NOW()),
    ('cn-1688-acrylic-08', 'TAOBAO', '批发办公室桌面文件收纳文件架可叠加多层置物架学生整理老师专用', 10.60, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01eCyjRc1kBSViIXGX3_!!1007574645-0-cib.jpg', 'https://detail.1688.com/offer/766110291276.html?spm=a26352.b28411319/2508.0.0&cosite=-&tracelog=p4p&_p_isad=1&clickid=194674d5268142c7b1c37dde66411811&sessionid=163e508d1e8d5123dd27fa65b8edc0f9', '商品属性
材质 
PET
产品类别 
文件架
层级数 
多层
尺寸 
315*245*70（mm）
品牌 
翰达
类型 
收纳架，置物架，整理架
货号 
HD25001
规格 
白色宽款一层,白色宽款二层,白色宽款三层,白色宽款四层,白色宽款五层,白色宽款六层,白色窄款一层,白色窄款二层,白色窄款三层,白色窄款四层,白色窄款五层,白色窄款六层,透明宽款一层,透明宽款二层,透明宽款三层,透明宽款四层,透明宽款五层,透明宽款六层,透明窄款一层,透明窄款二层,透明窄款三层,透明窄款四层,透明窄款五层,透明窄款六层
型号 
HD25001
加印LOGO 
可以
加工定制 
否
层数 
一层
层间距 
7（cm）
是否跨境出口专供货源 
否
贸易属性 
内贸+外贸
款式 
桌面款
颜色 
白色或透明 可定制颜色
包装信息
商品件重尺
规格 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)
白色宽款一层 31.50 24.50 7 5402.250 464
白色宽款二层 31.50 24.50 14 10804.500 928
白色宽款三层 31.50 24.50 21 16206.750 1392
白色宽款四层 31.50 24.50 28 21609 1856
白色宽款五层 31.50 24.50 35 27011.250 2320
白色宽款六层 31.50 24.50 42 32413.500 2784
白色窄款一层 24.50 31.50 14 10804.500 464
白色窄款二层 24.50 31.50 14 10804.500 928
白色窄款三层 24.50 31.50 21 16206.750 1392
白色窄款四层 24.50 31.50 28 21609 1856
白色窄款五层 24.50 31.50 35 27011.250 2320
白色窄款六层 31.50 24.50 42 32413.500 2784
透明宽款一层 31.50 24.50 7 5402.250 445
透明宽款二层 31.50 24.50 14 10804.500 890
透明宽款三层 31.50 24.50 21 16206.750 1335
透明宽款四层 31.50 24.50 28 21609 1780
透明宽款五层 31.50 24.50 35 27011.250 2225
透明宽款六层 31.50 24.50 7 5402.250 2670
透明窄款一层 24.50 31.50 7 5402.250 445
透明窄款二层 24.50 31.50 14 10804.500 890
透明窄款三层 24.50 31.50 21 16206.750 1335
透明窄款四层 24.50 31.50 28 21609 1780
透明窄款五层 24.50 31.50 35 27011.250 2225
透明窄款六层 24.50 31.50 42 32413.500 2670', '["https://cbu01.alicdn.com/img/ibank/O1CN01eCyjRc1kBSViIXGX3_!!1007574645-0-cib.jpg"]'::jsonb, '{"fixedKeyword": "亚克力透明收纳架", "sourceMarketplace": "1688", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, '{"priceText": "白色宽款一层10.6元\n白色宽款二层20.57元\n白色宽款三层30元\n白色宽款四层39.27元\n白色宽款五层49元\n白色宽款六层58.2元\n白色窄款一层10.6元\n白色窄款二层20.57元\n白色窄款三层30元\n白色窄款四层39.27元\n白色窄款五层49元\n白色窄款六层58.2元\n透明宽款一层10.6元\n透明宽款二层20.57元\n透明宽款三层30元\n透明宽款四层39.27元\n透明宽款五层49元\n透明宽款六层58.2元\n透明窄款一层10.6元\n透明窄款二层20.57元\n透明窄款三层30元\n透明窄款四层39.27元\n透明窄款五层49元\n透明窄款六层58.2元", "shippingText": "4元", "basePriceCny": 10.6}'::jsonb, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "白色宽款一层10.6元\n白色宽款二层20.57元\n白色宽款三层30元\n白色宽款四层39.27元\n白色宽款五层49元\n白色宽款六层58.2元\n白色窄款一层10.6元\n白色窄款二层20.57元\n白色窄款三层30元\n白色窄款四层39.27元\n白色窄款五层49元\n白色窄款六层58.2元\n透明宽款一层10.6元\n透明宽款二层20.57元\n透明宽款三层30元\n透明宽款四层39.27元\n透明宽款五层49元\n透明宽款六层58.2元\n透明窄款一层10.6元\n透明窄款二层20.57元\n透明窄款三层30元\n透明窄款四层39.27元\n透明窄款五层49元\n透明窄款六层58.2元", "shippingText": "4元", "detailText": "商品属性\n材质 \nPET\n产品类别 \n文件架\n层级数 \n多层\n尺寸 \n315*245*70（mm）\n品牌 \n翰达\n类型 \n收纳架，置物架，整理架\n货号 \nHD25001\n规格 \n白色宽款一层,白色宽款二层,白色宽款三层,白色宽款四层,白色宽款五层,白色宽款六层,白色窄款一层,白色窄款二层,白色窄款三层,白色窄款四层,白色窄款五层,白色窄款六层,透明宽款一层,透明宽款二层,透明宽款三层,透明宽款四层,透明宽款五层,透明宽款六层,透明窄款一层,透明窄款二层,透明窄款三层,透明窄款四层,透明窄款五层,透明窄款六层\n型号 \nHD25001\n加印LOGO \n可以\n加工定制 \n否\n层数 \n一层\n层间距 \n7（cm）\n是否跨境出口专供货源 \n否\n贸易属性 \n内贸+外贸\n款式 \n桌面款\n颜色 \n白色或透明 可定制颜色\n包装信息\n商品件重尺\n规格 长(cm) 宽(cm) 高(cm) 体积(cm³) 重量(g)\n白色宽款一层 31.50 24.50 7 5402.250 464\n白色宽款二层 31.50 24.50 14 10804.500 928\n白色宽款三层 31.50 24.50 21 16206.750 1392\n白色宽款四层 31.50 24.50 28 21609 1856\n白色宽款五层 31.50 24.50 35 27011.250 2320\n白色宽款六层 31.50 24.50 42 32413.500 2784\n白色窄款一层 24.50 31.50 14 10804.500 464\n白色窄款二层 24.50 31.50 14 10804.500 928\n白色窄款三层 24.50 31.50 21 16206.750 1392\n白色窄款四层 24.50 31.50 28 21609 1856\n白色窄款五层 24.50 31.50 35 27011.250 2320\n白色窄款六层 31.50 24.50 42 32413.500 2784\n透明宽款一层 31.50 24.50 7 5402.250 445\n透明宽款二层 31.50 24.50 14 10804.500 890\n透明宽款三层 31.50 24.50 21 16206.750 1335\n透明宽款四层 31.50 24.50 28 21609 1780\n透明宽款五层 31.50 24.50 35 27011.250 2225\n透明宽款六层 31.50 24.50 7 5402.250 2670\n透明窄款一层 24.50 31.50 7 5402.250 445\n透明窄款二层 24.50 31.50 14 10804.500 890\n透明窄款三层 24.50 31.50 21 16206.750 1335\n透明窄款四层 24.50 31.50 28 21609 1780\n透明窄款五层 24.50 31.50 35 27011.250 2225\n透明窄款六层 24.50 31.50 42 32413.500 2670", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, NOW(), NOW()),
    ('cn-1688-acrylic-09', 'TAOBAO', '桌面文件架A4纸办公室多层收纳盒亚克力书架置物架资料栏透明叠', 4.90, NULL, 'https://cbu01.alicdn.com/img/ibank/O1CN01YY5cay1K3VtDcn1tz_!!2213293631108-0-cib.jpg', 'https://detail.1688.com/offer/912180445727.html?spm=a26352.b28411319/2508.0.0', '商品属性
材质 
塑料
产品类别 
文件架
层级数 
5层
尺寸 
其他（mm）
品牌 
华盈家尚
收容量 
25
类型 
文件栏
货号 
250416
规格 
横款款1层【加厚高透-强力承重】,横款款2层【加厚高透-强力承重】,横款款3层【加厚高透-强力承重】,横款款4层【加厚高透-强力承重】,竖款1层【加厚高透-强力承重】,竖款2层【加厚高透-强力承重】,竖款3层【加厚高透-强力承重】,竖款4层【加厚高透-强力承重】
型号 
1
加印LOGO 
不可以
加工定制 
否
层数 
5
层间距 
其他（cm）
是否跨境出口专供货源 
否
包装信息
商品件重尺
规格 重量(g)
横款款1层【加厚高透-强力承重】 600
横款款2层【加厚高透-强力承重】 1200
横款款3层【加厚高透-强力承重】 2000
横款款4层【加厚高透-强力承重】 2600
竖款1层【加厚高透-强力承重】 600
竖款2层【加厚高透-强力承重】 1200
竖款3层【加厚高透-强力承重】 2000
竖款4层【加厚高透-强力承重】 2600', '["https://cbu01.alicdn.com/img/ibank/O1CN01YY5cay1K3VtDcn1tz_!!2213293631108-0-cib.jpg"]'::jsonb, '{"fixedKeyword": "亚克力透明收纳架", "sourceMarketplace": "1688", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, '{"priceText": "横款款1层【加厚高透-强力承重】4.9元\n横款款2层【加厚高透-强力承重】9.8元\n横款款3层【加厚高透-强力承重】15.8元\n横款款4层【加厚高透-强力承重】20.8元\n竖款1层【加厚高透-强力承重】4.9元\n竖款2层【加厚高透-强力承重】9.8元\n竖款3层【加厚高透-强力承重】15.8元\n竖款4层【加厚高透-强力承重】20.8元", "shippingText": "3元", "basePriceCny": 4.9}'::jsonb, '{"source": "csv-seed", "sourceMarketplace": "1688", "priceText": "横款款1层【加厚高透-强力承重】4.9元\n横款款2层【加厚高透-强力承重】9.8元\n横款款3层【加厚高透-强力承重】15.8元\n横款款4层【加厚高透-强力承重】20.8元\n竖款1层【加厚高透-强力承重】4.9元\n竖款2层【加厚高透-强力承重】9.8元\n竖款3层【加厚高透-强力承重】15.8元\n竖款4层【加厚高透-强力承重】20.8元", "shippingText": "3元", "detailText": "商品属性\n材质 \n塑料\n产品类别 \n文件架\n层级数 \n5层\n尺寸 \n其他（mm）\n品牌 \n华盈家尚\n收容量 \n25\n类型 \n文件栏\n货号 \n250416\n规格 \n横款款1层【加厚高透-强力承重】,横款款2层【加厚高透-强力承重】,横款款3层【加厚高透-强力承重】,横款款4层【加厚高透-强力承重】,竖款1层【加厚高透-强力承重】,竖款2层【加厚高透-强力承重】,竖款3层【加厚高透-强力承重】,竖款4层【加厚高透-强力承重】\n型号 \n1\n加印LOGO \n不可以\n加工定制 \n否\n层数 \n5\n层间距 \n其他（cm）\n是否跨境出口专供货源 \n否\n包装信息\n商品件重尺\n规格 重量(g)\n横款款1层【加厚高透-强力承重】 600\n横款款2层【加厚高透-强力承重】 1200\n横款款3层【加厚高透-强力承重】 2000\n横款款4层【加厚高透-强力承重】 2600\n竖款1层【加厚高透-强力承重】 600\n竖款2层【加厚高透-强力承重】 1200\n竖款3层【加厚高透-强力承重】 2000\n竖款4层【加厚高透-强力承重】 2600", "fixedKeyword": "亚克力透明收纳架", "sourceOverseasId": "amz-acrylic-03"}'::jsonb, NOW(), NOW())
ON CONFLICT (product_id) DO UPDATE SET
    platform = EXCLUDED.platform,
    title = EXCLUDED.title,
    price = EXCLUDED.price,
    brand = EXCLUDED.brand,
    image = EXCLUDED.image,
    link = EXCLUDED.link,
    description = EXCLUDED.description,
    gallery_json = EXCLUDED.gallery_json,
    attributes_json = EXCLUDED.attributes_json,
    sku_data_json = EXCLUDED.sku_data_json,
    raw_data_json = EXCLUDED.raw_data_json,
    updated_at = NOW();

