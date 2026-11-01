/**
 * PostCSS pipeline.
 *
 * Vite runs every .css file through PostCSS, and these two plugins do the work:
 *   - tailwindcss  reads tailwind.config.js, scans the `content` globs, and replaces the
 *                  @tailwind directives in index.css with the utility classes actually used.
 *   - autoprefixer adds vendor prefixes (-webkit-, -moz-) based on the browsers in the
 *                  browserslist default, so you never hand-write them.
 *
 * Without this file Tailwind's @tailwind directives would be shipped to the browser verbatim
 * and nothing would be styled.
 */
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
