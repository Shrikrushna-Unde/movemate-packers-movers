import { useState } from 'react';
import toast from 'react-hot-toast';
import { Eye, EyeOff, Pencil, Plus } from 'lucide-react';

import Button from '../ui/Button.jsx';
import { Input, Select, Textarea } from '../ui/Field.jsx';
import { Badge, Card } from '../ui/Card.jsx';
import { Alert, Spinner } from '../ui/Feedback.jsx';
import Modal from '../ui/Modal.jsx';
import ServiceIcon from '../ui/ServiceIcon.jsx';
import { adminApi } from '../../api/endpoints.js';
import { useApiData } from '../../hooks/useApiData.js';
import { formatCurrency } from '../../utils/format.js';

/** The icon keys the frontend knows how to render - see ServiceIcon. */
const ICON_OPTIONS = [
  { value: 'home', label: 'Home' },
  { value: 'building', label: 'Building (office)' },
  { value: 'car', label: 'Car / vehicle' },
  { value: 'truck', label: 'Truck' },
  { value: 'box', label: 'Box / packing' },
  { value: 'warehouse', label: 'Warehouse / storage' },
];

const EMPTY_FORM = {
  slug: '',
  title: '',
  shortDescription: '',
  description: '',
  icon: 'home',
  startingPrice: '',
  displayOrder: 0,
  active: true,
};

/**
 * Catalogue management: create, edit and show/hide services.
 *
 * One form handles both create and edit, which is why the backend exposes an "upsert" DTO.
 * `editing === null` means create; otherwise the form is prefilled from the row and the save
 * goes to PUT instead of POST. Two separate forms would be two places to add the next field.
 */
export default function AdminServices() {
  const { data: services, loading, error, reload } = useApiData(() => adminApi.services());

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState(null);

  const startCreate = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setFieldErrors({});
    setOpen(true);
  };

  const startEdit = (service) => {
    setEditing(service);
    setForm({
      slug: service.slug,
      title: service.title,
      shortDescription: service.shortDescription,
      description: service.description,
      icon: service.icon,
      // The API sends a number; the text input needs a string.
      startingPrice: String(service.startingPrice),
      displayOrder: service.displayOrder,
      active: service.active,
    });
    setFieldErrors({});
    setOpen(true);
  };

  const handleChange = (event) => {
    const { name, type, value, checked } = event.target;
    setForm((previous) => ({ ...previous, [name]: type === 'checkbox' ? checked : value }));
    setFieldErrors((previous) => ({ ...previous, [name]: undefined }));
  };

  const save = async () => {
    setSaving(true);
    setFieldErrors({});

    const payload = {
      ...form,
      startingPrice: Number(form.startingPrice),
      displayOrder: Number(form.displayOrder) || 0,
    };

    try {
      if (editing) {
        await adminApi.updateService(editing.id, payload);
        toast.success(`${payload.title} updated`);
      } else {
        await adminApi.createService(payload);
        toast.success(`${payload.title} created`);
      }
      setOpen(false);
      reload();
    } catch (err) {
      // A duplicate slug comes back as 409 with a message naming the clash.
      if (err.fieldErrors) setFieldErrors(err.fieldErrors);
      else toast.error(err.message);
    } finally {
      setSaving(false);
    }
  };

  const toggle = async (service) => {
    setBusyId(service.id);
    try {
      await adminApi.toggleService(service.id);
      toast.success(service.active ? `${service.title} hidden` : `${service.title} is now live`);
      reload();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  };

  if (loading) return <Spinner label="Loading catalogue" />;
  if (error) return <Alert type="error">{error}</Alert>;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-600">
          {services.length} services · hidden ones disappear from the website but keep their
          existing bookings intact.
        </p>
        <Button onClick={startCreate}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          Add service
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {services.map((service) => (
          <Card key={service.id} className={`p-5 ${service.active ? '' : 'opacity-60'}`}>
            <div className="flex items-start justify-between gap-3">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700">
                <ServiceIcon name={service.icon} className="h-5 w-5" />
              </span>
              {service.active ? (
                <Badge className="bg-emerald-100 text-emerald-800">Live</Badge>
              ) : (
                <Badge className="bg-slate-200 text-slate-600">Hidden</Badge>
              )}
            </div>

            <h3 className="mt-4 font-semibold text-slate-900">{service.title}</h3>
            <p className="mt-1 text-xs text-slate-500">
              /{service.slug} · order {service.displayOrder}
            </p>
            <p className="mt-2 line-clamp-2 text-sm text-slate-600">{service.shortDescription}</p>

            <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3">
              <span className="text-sm font-bold text-slate-900">
                {formatCurrency(service.startingPrice)}
              </span>
              <div className="flex gap-1">
                <Button variant="ghost" size="sm" onClick={() => startEdit(service)}>
                  <Pencil className="h-4 w-4" aria-hidden="true" />
                  Edit
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  loading={busyId === service.id}
                  onClick={() => toggle(service)}
                  aria-label={service.active ? 'Hide service' : 'Show service'}
                >
                  {service.active ? (
                    <EyeOff className="h-4 w-4" aria-hidden="true" />
                  ) : (
                    <Eye className="h-4 w-4" aria-hidden="true" />
                  )}
                </Button>
              </div>
            </div>
          </Card>
        ))}
      </div>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? `Edit ${editing.title}` : 'Add a service'}
        description="These fields drive the public services page and the detail page."
        footer={
          <>
            <Button variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save} loading={saving}>
              {editing ? 'Save changes' : 'Create service'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input
            label="Title"
            name="title"
            value={form.title}
            onChange={handleChange}
            required
            error={fieldErrors.title}
          />
          <Input
            label="URL slug"
            name="slug"
            value={form.slug}
            onChange={handleChange}
            required
            placeholder="home-shifting"
            error={fieldErrors.slug}
            hint="Lowercase words separated by single hyphens. Becomes /services/your-slug"
          />
          <Textarea
            label="Short description"
            name="shortDescription"
            rows={2}
            value={form.shortDescription}
            onChange={handleChange}
            required
            error={fieldErrors.shortDescription}
            hint="One line, shown on the service cards"
          />
          <Textarea
            label="Full description"
            name="description"
            rows={5}
            value={form.description}
            onChange={handleChange}
            required
            error={fieldErrors.description}
            hint="Shown on the detail page. Up to 1500 characters."
          />
          <div className="grid gap-4 sm:grid-cols-3">
            <Select
              label="Icon"
              name="icon"
              value={form.icon}
              onChange={handleChange}
              options={ICON_OPTIONS}
              error={fieldErrors.icon}
            />
            <Input
              label="Starting price"
              name="startingPrice"
              type="number"
              min="0"
              value={form.startingPrice}
              onChange={handleChange}
              required
              error={fieldErrors.startingPrice}
            />
            <Input
              label="Display order"
              name="displayOrder"
              type="number"
              min="0"
              value={form.displayOrder}
              onChange={handleChange}
              error={fieldErrors.displayOrder}
            />
          </div>

          <label className="flex items-center gap-2.5 text-sm text-slate-700">
            <input
              type="checkbox"
              name="active"
              checked={form.active}
              onChange={handleChange}
              className="h-4 w-4 rounded border-slate-300 text-brand-700 focus:ring-brand-500"
            />
            Visible on the website
          </label>
        </div>
      </Modal>
    </div>
  );
}
