package com.movemate.service;

import com.movemate.domain.Testimonial;
import com.movemate.domain.User;
import com.movemate.dto.PageResponse;
import com.movemate.dto.TestimonialRequest;
import com.movemate.dto.TestimonialResponse;
import com.movemate.exception.ResourceNotFoundException;
import com.movemate.repository.TestimonialRepository;
import com.movemate.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer reviews, with an admin moderation step.
 */
@Service
public class TestimonialService {

    private final TestimonialRepository testimonialRepository;
    private final UserRepository userRepository;

    public TestimonialService(TestimonialRepository testimonialRepository,
                              UserRepository userRepository) {
        this.testimonialRepository = testimonialRepository;
        this.userRepository = userRepository;
    }

    /** The public feed: approved reviews only. */
    @Transactional(readOnly = true)
    public List<TestimonialResponse> findApproved() {
        return testimonialRepository.findByApprovedTrueOrderByCreatedAtDesc().stream()
                .map(TestimonialResponse::from)
                .toList();
    }

    /**
     * Submits a review.
     *
     * <p>Only logged-in customers may post, which is a deliberate anti-spam measure: an
     * anonymous review endpoint needs a CAPTCHA, rate limiting and IP reputation to survive
     * contact with the internet. Requiring an account gets most of that for free.
     *
     * <p>The author name is taken from the authenticated account, not from the request body,
     * so nobody can post a review signed "Tata Motors".
     */
    @Transactional
    public TestimonialResponse submit(Long userId, TestimonialRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Testimonial testimonial = new Testimonial(
                author,
                author.getFullName(),
                request.city().trim(),
                request.rating(),
                request.message().trim());
        // approved stays false - see the field default on the entity.

        return TestimonialResponse.from(testimonialRepository.save(testimonial));
    }

    /** Admin moderation queue: everything, newest first. */
    @Transactional(readOnly = true)
    public PageResponse<TestimonialResponse> findAllForAdmin(Pageable pageable) {
        return PageResponse.from(
                testimonialRepository.findAllByOrderByCreatedAtDesc(pageable),
                TestimonialResponse::from);
    }

    /**
     * Approves or un-approves a review.
     *
     * <p>Takes the desired value as a parameter rather than toggling, because an admin
     * double-clicking a toggle in a flaky network should not flip the state twice. An explicit
     * value makes the operation idempotent - the same principle behind PUT in REST.
     */
    @Transactional
    public TestimonialResponse setApproved(Long id, boolean approved) {
        Testimonial testimonial = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial", "id", id));
        testimonial.setApproved(approved);
        return TestimonialResponse.from(testimonial);
    }

    @Transactional
    public void delete(Long id) {
        if (!testimonialRepository.existsById(id)) {
            throw new ResourceNotFoundException("Testimonial", "id", id);
        }
        // A review really is deleted rather than soft-deleted: unlike catalogue data, nothing
        // references it, and an admin removing abusive content expects it gone.
        testimonialRepository.deleteById(id);
    }
}
