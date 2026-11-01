-- ===================================================================
--  V2__seed_catalog.sql
--
--  Reference data the website cannot render without: the service catalogue
--  and a few approved reviews. Seeding it as a migration (instead of a manual
--  INSERT someone runs by hand) means a brand-new database - a reviewer's
--  laptop, a fresh Railway instance - comes up fully populated.
--
--  The admin USER is NOT seeded here: its password must be BCrypt-hashed, and
--  hashing belongs in Java. See config/DataSeeder.java.
-- ===================================================================

INSERT INTO moving_services
    (slug, title, short_description, description, icon, starting_price, display_order, active, created_at)
VALUES
    ('home-shifting',
     'Home Shifting',
     'Complete door-to-door household relocation with packing, loading and unpacking.',
     'Our most requested service. A dedicated supervisor surveys your home, we pack every item with multi-layer bubble wrap and corrugated sheets, dismantle large furniture, load into a closed-body truck, and reassemble everything at your new address. Pricing scales with your home size and distance, so a 1 BHK local move never subsidises a 3 BHK intercity one.',
     'home', 3100.00, 1, TRUE, NOW(6)),

    ('office-relocation',
     'Office Relocation',
     'Weekend and overnight office moves planned to keep your downtime near zero.',
     'We move workstations, server racks, conference furniture and records with asset tagging so every item is accounted for at the destination. Moves are scheduled on weekends or overnight, IT equipment is packed in anti-static wrap, and we hand over a numbered inventory sheet your facilities team can sign off against.',
     'building', 13500.00, 2, TRUE, NOW(6)),

    ('vehicle-transport',
     'Car & Bike Transport',
     'Enclosed carrier transport for two-wheelers and cars across India.',
     'Your vehicle travels in a covered single or multi-car carrier - never driven on its own wheels - so the odometer and tyres stay untouched. We drain fuel to the legally permitted level, photograph the vehicle from eight angles before loading, and share those photos with you as the pre-dispatch condition report.',
     'car', 5400.00, 3, TRUE, NOW(6)),

    ('domestic-relocation',
     'Domestic Relocation',
     'Intercity moves with GPS-tracked vehicles and transit insurance.',
     'For moves beyond 150 km we run dedicated or shared containers depending on volume. Every vehicle is GPS tracked, you get a named driver and coordinator, and transit insurance can be added at 2.5 percent of your declared goods value - the industry standard rate.',
     'truck', 16000.00, 4, TRUE, NOW(6)),

    ('packing-unpacking',
     'Packing & Unpacking',
     'Material-only or labour-only packing when you arrange your own transport.',
     'Some customers already have a vehicle and only need professional packing. We bring five-ply cartons, bubble wrap, stretch film, foam sheets and wardrobe boxes, pack room by room with colour-coded labels, and return for unpacking at your destination on a scheduled day.',
     'box', 2200.00, 5, TRUE, NOW(6)),

    ('warehousing-storage',
     'Warehousing & Storage',
     'Clean, pest-controlled short and long-term storage charged per day.',
     'When possession of your new home is delayed, your goods sit in a racked, pest-controlled warehouse with CCTV and a per-item inventory. Storage is billed per day rather than per month, so a nine-day gap costs nine days - not a full month.',
     'warehouse', 3500.00, 6, TRUE, NOW(6));

INSERT INTO testimonials
    (author_name, city, rating, message, approved, created_at)
VALUES
    ('Rohan Deshmukh', 'Pune', 5,
     'Shifted a 2 BHK from Kothrud to Hinjewadi. The team arrived at 8 AM sharp, packed everything in four hours and nothing was damaged. The online estimate was within 300 rupees of the final invoice.',
     TRUE, NOW(6)),
    ('Sneha Kulkarni', 'Mumbai', 5,
     'I was nervous about moving my piano. They built a custom crate for it on site. Genuinely professional and the tracking link meant I always knew where the truck was.',
     TRUE, NOW(6)),
    ('Imran Shaikh', 'Nagpur', 4,
     'Office move of 40 workstations done over a weekend. We were operational on Monday morning. Lost one keyboard, which they replaced without any argument.',
     TRUE, NOW(6)),
    ('Priya Nair', 'Bengaluru', 5,
     'Car transport from Bengaluru to Pune. The eight-angle photo report before loading gave me a lot of confidence. Delivered a day earlier than promised.',
     TRUE, NOW(6)),
    ('Amit Verma', 'Delhi', 5,
     'Used the warehousing service for 11 days because my flat possession got delayed. Charged me exactly 11 days, not a whole month like the other quotes I had.',
     TRUE, NOW(6)),
    ('Kavita Joshi', 'Nashik', 4,
     'Clear pricing with no surprise charges at delivery, which is rare in this industry. The quote breakdown on the website showing GST separately is a nice touch.',
     TRUE, NOW(6));
