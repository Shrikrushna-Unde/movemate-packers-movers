import { useState } from 'react';
import toast from 'react-hot-toast';
import { Clock, Mail, MapPin, MessageCircle, Phone, Send } from 'lucide-react';

import Button from '../components/ui/Button.jsx';
import { Input, Textarea } from '../components/ui/Field.jsx';
import { Card, SectionHeading } from '../components/ui/Card.jsx';
import { Alert } from '../components/ui/Feedback.jsx';
import { contactApi } from '../api/endpoints.js';
import { CONTACT_EMAIL, CONTACT_PHONE, WHATSAPP_URL } from '../utils/constants.js';

const EMPTY = { name: '', email: '', phone: '', subject: '', message: '' };

export default function Contact() {
  const [form, setForm] = useState(EMPTY);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => ({ ...previous, [name]: undefined }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setFieldErrors({});

    try {
      await contactApi.submit({
        ...form,
        // The phone field is optional. Sending null rather than "" matters: the backend's
        // @Pattern skips null but DOES validate an empty string, so "" would be rejected as an
        // invalid phone number. A subtle Bean Validation behaviour worth knowing.
        phone: form.phone || null,
      });
      setSent(true);
      setForm(EMPTY);
      toast.success('Message sent. We will reply within 2 hours.');
    } catch (error) {
      if (error.fieldErrors) {
        setFieldErrors(error.fieldErrors);
      } else {
        toast.error(error.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container-page py-12 sm:py-16">
      <SectionHeading
        align="left"
        eyebrow="Contact"
        title="Talk to a move coordinator"
        subtitle="For a price, the quote calculator is faster than any phone call. For everything else, here is how to reach us."
      />

      <div className="mt-10 grid gap-8 lg:grid-cols-[1fr_1.3fr] lg:items-start">
        {/* Contact details */}
        <div className="space-y-5">
          <Card className="p-6">
            <h2 className="text-base font-bold text-slate-900">Reach us directly</h2>
            <ul className="mt-5 space-y-4 text-sm">
              <li className="flex items-start gap-3">
                <Phone className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" aria-hidden="true" />
                <div>
                  <p className="font-medium text-slate-800">Call us</p>
                  <a href={`tel:${CONTACT_PHONE}`} className="text-brand-700 hover:underline">
                    {CONTACT_PHONE}
                  </a>
                </div>
              </li>
              <li className="flex items-start gap-3">
                <MessageCircle
                  className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600"
                  aria-hidden="true"
                />
                <div>
                  <p className="font-medium text-slate-800">WhatsApp</p>
                  <a
                    href={WHATSAPP_URL}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-brand-700 hover:underline"
                  >
                    Chat on {CONTACT_PHONE}
                  </a>
                </div>
              </li>
              <li className="flex items-start gap-3">
                <Mail className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" aria-hidden="true" />
                <div>
                  <p className="font-medium text-slate-800">Email</p>
                  <a href={`mailto:${CONTACT_EMAIL}`} className="text-brand-700 hover:underline">
                    {CONTACT_EMAIL}
                  </a>
                </div>
              </li>
              <li className="flex items-start gap-3">
                <MapPin className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" aria-hidden="true" />
                <div>
                  <p className="font-medium text-slate-800">Head office</p>
                  <p className="text-slate-600">Office 204, Baner Road, Pune 411045</p>
                </div>
              </li>
              <li className="flex items-start gap-3">
                <Clock className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" aria-hidden="true" />
                <div>
                  <p className="font-medium text-slate-800">Operating hours</p>
                  <p className="text-slate-600">Mon-Sat, 8 AM to 8 PM. Moves run on Sundays too.</p>
                </div>
              </li>
            </ul>
          </Card>
        </div>

        {/* Form */}
        <Card className="p-6 sm:p-8">
          {sent && (
            <Alert type="success" title="Thanks for getting in touch" className="mb-6">
              Your message is with our team. Expect a reply within two hours during operating
              hours.
            </Alert>
          )}

          <form onSubmit={handleSubmit} className="grid gap-5 sm:grid-cols-2" noValidate>
            <Input
              label="Your name"
              name="name"
              value={form.name}
              onChange={handleChange}
              required
              error={fieldErrors.name}
            />
            <Input
              label="Email"
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              required
              error={fieldErrors.email}
            />
            <Input
              label="Phone (optional)"
              name="phone"
              inputMode="numeric"
              maxLength={10}
              value={form.phone}
              onChange={handleChange}
              error={fieldErrors.phone}
            />
            <Input
              label="Subject"
              name="subject"
              value={form.subject}
              onChange={handleChange}
              required
              placeholder="Question about intercity move"
              error={fieldErrors.subject}
            />
            <Textarea
              className="sm:col-span-2"
              label="Message"
              name="message"
              rows={5}
              value={form.message}
              onChange={handleChange}
              required
              placeholder="Tell us what you are moving, from where to where, and your rough timeline."
              error={fieldErrors.message}
              hint="Minimum 10 characters"
            />

            <div className="sm:col-span-2">
              <Button type="submit" loading={submitting} size="lg">
                <Send className="h-4 w-4" aria-hidden="true" />
                {submitting ? 'Sending...' : 'Send message'}
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </div>
  );
}
