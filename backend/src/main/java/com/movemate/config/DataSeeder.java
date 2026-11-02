package com.movemate.config;

import com.movemate.domain.MovingService;
import com.movemate.domain.Role;
import com.movemate.domain.Testimonial;
import com.movemate.domain.User;
import com.movemate.repository.MovingServiceRepository;
import com.movemate.repository.TestimonialRepository;
import com.movemate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Creates the bootstrap accounts and, when necessary, the reference data.
 *
 * <p>{@link CommandLineRunner} runs once, after the Spring context is ready and after Flyway
 * has migrated, but before the app starts serving traffic - exactly the window we need.
 *
 * <p>Everything here is <b>idempotent</b>: it checks before it inserts. That matters because a
 * container restarts, redeploys and scales, so this code runs many times against the same
 * database. An unconditional INSERT would crash on the UNIQUE email constraint on the second
 * boot and the app would never come up.
 *
 * <p>That same property is what lets one class serve two very different environments:
 * <ul>
 *   <li><b>MySQL</b> - {@code V2__seed_catalog.sql} already inserted the services and reviews,
 *       so the catalogue methods below find a non-empty table and do nothing.</li>
 *   <li><b>H2 (the {@code dev} profile)</b> - Flyway is disabled and Hibernate created an empty
 *       schema, so these methods populate it.</li>
 * </ul>
 * No duplicated seed data in two SQL dialects, and no environment-specific branching.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** A ready-made customer login so a reviewer can see the dashboard without signing up. */
    private static final String DEMO_EMAIL = "customer@movemate.in";
    private static final String DEMO_PASSWORD = "Customer@123";

    private final UserRepository userRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final TestimonialRepository testimonialRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public DataSeeder(UserRepository userRepository,
                      MovingServiceRepository movingServiceRepository,
                      TestimonialRepository testimonialRepository,
                      PasswordEncoder passwordEncoder,
                      AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.movingServiceRepository = movingServiceRepository;
        this.testimonialRepository = testimonialRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    /**
     * {@code @Transactional} wraps every insert in one database transaction, so a failure
     * halfway cannot leave the admin created and the catalogue missing.
     */
    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedDemoCustomer();
        seedCatalogue();
        seedTestimonials();
    }

    /**
     * Creates the admin account, or re-syncs its password when the configured one has changed.
     *
     * <p>The re-sync is the important half. An earlier version simply returned when the account
     * already existed, which made {@code ADMIN_PASSWORD} a write-once variable: it was honoured
     * at creation and silently ignored on every boot afterwards. Change it in the hosting
     * dashboard, redeploy, and you are locked out of your own admin panel - with a perfectly
     * green deploy and nothing in the logs to say why.
     *
     * <p>Reconciling instead makes the environment variable authoritative, which is what its
     * name promises. That is safe here precisely because this application has no self-service
     * change-password screen: configuration is the only source of truth for this account, so
     * there is no user-chosen password for it to overwrite. Were that to change, this method
     * would have to change with it.
     */
    private void seedAdmin() {
        String email = adminProperties.email();

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User admin = existing.get();

            // matches() hashes the candidate with the stored salt and compares - never compare
            // an encoded password with equals(), because BCrypt generates a fresh salt each
            // time and two hashes of the same password are never byte-identical.
            if (!passwordEncoder.matches(adminProperties.password(), admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(adminProperties.password()));
                userRepository.save(admin);
                log.info("Admin password re-synced from configuration for {}", email);
            }
            return;
        }

        User admin = new User(
                adminProperties.fullName(),
                email,
                "8999115935",
                // The one place plaintext touches the app, and it is hashed on the same line.
                passwordEncoder.encode(adminProperties.password()),
                Role.ROLE_ADMIN);
        userRepository.save(admin);

        // Log the email, never the password. Platform logs are visible to anyone with
        // dashboard access and are retained long after you have forgotten this line exists.
        log.info("Seeded admin account: {}", email);
    }

    private void seedDemoCustomer() {
        if (userRepository.existsByEmail(DEMO_EMAIL)) {
            return;
        }
        User demo = new User(
                "Demo Customer",
                DEMO_EMAIL,
                "9876543210",
                passwordEncoder.encode(DEMO_PASSWORD),
                Role.ROLE_CUSTOMER);
        userRepository.save(demo);
        log.info("Seeded demo customer account: {}", DEMO_EMAIL);
    }

    /**
     * Fills the service catalogue only when it is empty.
     *
     * <p>{@code count() > 0} rather than checking each slug: the two sources of this data
     * (the Flyway migration and this method) insert the same six rows as a set, so the
     * table is either populated or it is not.
     */
    private void seedCatalogue() {
        if (movingServiceRepository.count() > 0) {
            return;
        }

        save("home-shifting", "Home Shifting",
                "Complete door-to-door household relocation with packing, loading and unpacking.",
                "Our most requested service. A dedicated supervisor surveys your home, we pack every "
                + "item with multi-layer bubble wrap and corrugated sheets, dismantle large furniture, "
                + "load into a closed-body truck, and reassemble everything at your new address. Pricing "
                + "scales with your home size and distance, so a 1 BHK local move never subsidises a "
                + "3 BHK intercity one.",
                "home", 3100, 1);

        save("office-relocation", "Office Relocation",
                "Weekend and overnight office moves planned to keep your downtime near zero.",
                "We move workstations, server racks, conference furniture and records with asset tagging "
                + "so every item is accounted for at the destination. Moves are scheduled on weekends or "
                + "overnight, IT equipment is packed in anti-static wrap, and we hand over a numbered "
                + "inventory sheet your facilities team can sign off against.",
                "building", 13500, 2);

        save("vehicle-transport", "Car & Bike Transport",
                "Enclosed carrier transport for two-wheelers and cars across India.",
                "Your vehicle travels in a covered single or multi-car carrier - never driven on its own "
                + "wheels - so the odometer and tyres stay untouched. We drain fuel to the legally "
                + "permitted level, photograph the vehicle from eight angles before loading, and share "
                + "those photos with you as the pre-dispatch condition report.",
                "car", 5400, 3);

        save("domestic-relocation", "Domestic Relocation",
                "Intercity moves with GPS-tracked vehicles and transit insurance.",
                "For moves beyond 150 km we run dedicated or shared containers depending on volume. Every "
                + "vehicle is GPS tracked, you get a named driver and coordinator, and transit insurance "
                + "can be added at 2.5 percent of your declared goods value - the industry standard rate.",
                "truck", 16000, 4);

        save("packing-unpacking", "Packing & Unpacking",
                "Material-only or labour-only packing when you arrange your own transport.",
                "Some customers already have a vehicle and only need professional packing. We bring "
                + "five-ply cartons, bubble wrap, stretch film, foam sheets and wardrobe boxes, pack room "
                + "by room with colour-coded labels, and return for unpacking at your destination on a "
                + "scheduled day.",
                "box", 2200, 5);

        save("warehousing-storage", "Warehousing & Storage",
                "Clean, pest-controlled short and long-term storage charged per day.",
                "When possession of your new home is delayed, your goods sit in a racked, pest-controlled "
                + "warehouse with CCTV and a per-item inventory. Storage is billed per day rather than per "
                + "month, so a nine-day gap costs nine days - not a full month.",
                "warehouse", 3500, 6);

        log.info("Seeded {} catalogue services", movingServiceRepository.count());
    }

    /** Small helper so the six calls above read as data rather than eight setter lines each. */
    private void save(String slug, String title, String shortDescription, String description,
                      String icon, long startingPrice, int displayOrder) {
        MovingService service = new MovingService();
        service.setSlug(slug);
        service.setTitle(title);
        service.setShortDescription(shortDescription);
        service.setDescription(description);
        service.setIcon(icon);
        // valueOf(long), not new BigDecimal(double) - the double constructor carries binary
        // rounding error into the value.
        service.setStartingPrice(BigDecimal.valueOf(startingPrice));
        service.setDisplayOrder(displayOrder);
        service.setActive(true);
        movingServiceRepository.save(service);
    }

    /** Sample approved reviews, again only when the table is empty. */
    private void seedTestimonials() {
        if (testimonialRepository.count() > 0) {
            return;
        }

        review("Rohan Deshmukh", "Pune", 5,
                "Shifted a 2 BHK from Kothrud to Hinjewadi. The team arrived at 8 AM sharp, packed "
                + "everything in four hours and nothing was damaged. The online estimate was within "
                + "300 rupees of the final invoice.");
        review("Sneha Kulkarni", "Mumbai", 5,
                "I was nervous about moving my piano. They built a custom crate for it on site. "
                + "Genuinely professional and the tracking link meant I always knew where the truck was.");
        review("Imran Shaikh", "Nagpur", 4,
                "Office move of 40 workstations done over a weekend. We were operational on Monday "
                + "morning. Lost one keyboard, which they replaced without any argument.");
        review("Priya Nair", "Bengaluru", 5,
                "Car transport from Bengaluru to Pune. The eight-angle photo report before loading gave "
                + "me a lot of confidence. Delivered a day earlier than promised.");
        review("Amit Verma", "Delhi", 5,
                "Used the warehousing service for 11 days because my flat possession got delayed. "
                + "Charged me exactly 11 days, not a whole month like the other quotes I had.");
        review("Kavita Joshi", "Nashik", 4,
                "Clear pricing with no surprise charges at delivery, which is rare in this industry. "
                + "The quote breakdown on the website showing GST separately is a nice touch.");

        log.info("Seeded {} testimonials", testimonialRepository.count());
    }

    private void review(String author, String city, int rating, String message) {
        // null user: these are seeded reviews with no account behind them, which is exactly
        // why Testimonial.user is nullable.
        Testimonial testimonial = new Testimonial(null, author, city, rating, message);
        testimonial.setApproved(true);
        testimonialRepository.save(testimonial);
    }
}
