import { useState } from 'react';
import Header from '../components/layout/Header';
import {
  IoChevronDown, IoChevronUp, IoCall, IoMail, IoGlobe,
  IoHelpCircle, IoShield, IoSwapHorizontal, IoCard, IoQrCode,
  IoWallet, IoChatbubbles, IoInformationCircle
} from 'react-icons/io5';

const faqs = [
  {
    category: 'Getting Started',
    icon: IoHelpCircle,
    questions: [
      { q: 'What is NUPI?', a: 'NUPI (Nepal Unified Payment Interface) is a real-time payment system that enables instant fund transfers between bank accounts through mobile devices. It is regulated by Nepal Rastra Bank (NRB).' },
      { q: 'How do I register on NUPI?', a: 'Download the NUPI app, enter your mobile number linked to your bank account, verify with OTP, set up your 6-digit MPIN, and link your bank account. You\'re ready to make payments!' },
      { q: 'Is NUPI free to use?', a: 'NUPI does not charge any fees for person-to-person transactions. Some merchant transactions may have nominal charges as per NRB guidelines.' },
    ],
  },
  {
    category: 'Payments',
    icon: IoSwapHorizontal,
    questions: [
      { q: 'What is the transaction limit?', a: 'Per transaction limit is Rs 1,00,000. Daily limit is Rs 2,00,000 and monthly limit is Rs 5,00,000. UPI Lite transactions are limited to Rs 500 per transaction with a wallet max of Rs 2,000.' },
      { q: 'How long does a transaction take?', a: 'NUPI transactions are processed in real-time and typically complete within a few seconds. If a transaction is pending, it will auto-resolve within 48 hours.' },
      { q: 'Can I reverse a transaction?', a: 'Completed transactions cannot be reversed directly. If you sent money to the wrong person, you can request them to send it back or raise a complaint through the app.' },
    ],
  },
  {
    category: 'Security',
    icon: IoShield,
    questions: [
      { q: 'Is NUPI safe to use?', a: 'Yes! NUPI uses end-to-end encryption, secure 2-factor authentication (OTP + MPIN), and is regulated by Nepal Rastra Bank. Your bank details are never shared with anyone.' },
      { q: 'What if I forget my MPIN?', a: 'You can reset your MPIN from the login screen by tapping "Forgot MPIN". You\'ll need to verify your identity through OTP and your bank debit card details.' },
      { q: 'What if my phone is lost?', a: 'Immediately call your bank to block UPI access. You can also use another device to deregister from NUPI. Your funds remain safe in your bank account.' },
    ],
  },
  {
    category: 'UPI Lite & Mandates',
    icon: IoWallet,
    questions: [
      { q: 'What is UPI Lite?', a: 'UPI Lite allows small value transactions (up to Rs 500) without entering MPIN. You need to top up your UPI Lite wallet from your linked bank account.' },
      { q: 'What are Mandates?', a: 'Mandates (Auto-pay) allow you to authorize recurring payments like subscriptions, EMIs, and bills. You can pause, resume, or revoke mandates anytime.' },
    ],
  },
];

export default function Help() {
  const [openFaq, setOpenFaq] = useState(null);
  const [search, setSearch] = useState('');

  const toggleFaq = (key) => setOpenFaq(prev => prev === key ? null : key);

  const filteredFaqs = search
    ? faqs.map(cat => ({
        ...cat,
        questions: cat.questions.filter(q =>
          q.q.toLowerCase().includes(search.toLowerCase()) ||
          q.a.toLowerCase().includes(search.toLowerCase())
        ),
      })).filter(cat => cat.questions.length > 0)
    : faqs;

  return (
    <div className="page-container pb-24">
      <Header title="Help & Support" showBack />

      <div className="px-4 mt-4 space-y-5">
        {/* Search */}
        <div className="relative">
          <IoHelpCircle className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input type="text" value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search FAQs..." className="input-field pl-9" />
        </div>

        {/* Contact Cards */}
        <div className="grid grid-cols-3 gap-3">
          <a href="tel:+97716000000" className="card text-center py-3 hover:shadow-md transition-shadow">
            <IoCall className="w-6 h-6 text-green-600 mx-auto mb-1" />
            <p className="text-xs font-medium">Call Us</p>
          </a>
          <a href="mailto:support@nupi.np" className="card text-center py-3 hover:shadow-md transition-shadow">
            <IoMail className="w-6 h-6 text-blue-600 mx-auto mb-1" />
            <p className="text-xs font-medium">Email</p>
          </a>
          <a href="https://nupi.np" target="_blank" rel="noopener noreferrer" className="card text-center py-3 hover:shadow-md transition-shadow">
            <IoGlobe className="w-6 h-6 text-purple-600 mx-auto mb-1" />
            <p className="text-xs font-medium">Website</p>
          </a>
        </div>

        {/* FAQ Sections */}
        {filteredFaqs.map((category, ci) => (
          <div key={category.category}>
            <div className="flex items-center gap-2 mb-2 px-1">
              <category.icon className="w-4 h-4 text-nupi-primary" />
              <p className="text-xs font-semibold text-nupi-text-muted uppercase tracking-wider">{category.category}</p>
            </div>
            <div className="card p-0 overflow-hidden divide-y divide-gray-100">
              {category.questions.map((faq, qi) => {
                const key = `${ci}-${qi}`;
                const isOpen = openFaq === key;
                return (
                  <div key={key}>
                    <button onClick={() => toggleFaq(key)}
                      className="w-full flex items-center justify-between px-4 py-3.5 text-left hover:bg-gray-50 transition-colors">
                      <span className="text-sm font-medium pr-4">{faq.q}</span>
                      {isOpen ? <IoChevronUp className="w-4 h-4 text-gray-400 flex-shrink-0" /> : <IoChevronDown className="w-4 h-4 text-gray-400 flex-shrink-0" />}
                    </button>
                    {isOpen && (
                      <div className="px-4 pb-3.5 animate-slide-up">
                        <p className="text-sm text-nupi-text-secondary leading-relaxed">{faq.a}</p>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        ))}

        {filteredFaqs.length === 0 && (
          <div className="text-center py-12">
            <IoHelpCircle className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-nupi-text-muted">No matching FAQs found</p>
          </div>
        )}

        {/* About */}
        <div className="card bg-gray-50">
          <div className="flex items-center gap-2 mb-2">
            <IoInformationCircle className="w-5 h-5 text-nupi-primary" />
            <p className="font-semibold text-sm">About NUPI</p>
          </div>
          <p className="text-xs text-nupi-text-secondary leading-relaxed">
            NUPI (Nepal Unified Payment Interface) is developed under the guidance of Nepal Rastra Bank (NRB)
            to facilitate real-time digital payments across Nepal. NUPI enables interoperable payments
            between banks and financial institutions using a simple UPI ID system.
          </p>
          <p className="text-xs text-nupi-text-muted mt-2">Version 1.0.0 • © 2024 NRB</p>
        </div>
      </div>
    </div>
  );
}
