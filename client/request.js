const axios = require('axios');
const { randomUUID } = require('crypto'); // Node.js 내장 모듈 사용

const BASE_URL = 'http://localhost:8080/api/v1/notifications';
// const BASE_URL = 'http://localhost:8080/api/notifications';

const testCases = {
    "회원 + 비회원 혼합, 템플릿 사용, 이메일 + 푸시 즉시 발송": {
        requester: { type: "SERVICE", id: "event-marketing-service" },
        recipients: {
            userIds: ["user-001", "user-002"],
            directRecipients: [
                { phoneNumber: null, email: "guest@example.com", deviceToken: null },
                { phoneNumber: null, email: null, deviceToken: "guest-device-token-xyz" }
            ]
        },
        notificationTypes: ["EMAIL", "PUSH"],
        senderInfos: {
            EMAIL: { senderEmailAddress: "no-reply@myproduct.com", senderName: "My Product" },
            PUSH: { senderName: "My Product App" }
        },
        template: {
            templateId: "NEW_PRODUCT_LAUNCH",
            templateParameters: {
                productName: "Smart Watch X",
                launchDate: "2025-07-01"
            }
        },
        scheduledAt: null,
        memo: "템플릿 기반 혼합 발송"
    },

    "다수 회원 대상, 직접 작성한 SMS, 예약 발송": {
        requester: { type: "ADMIN", id: "admin-sales" },
        recipients: {
            userIds: ["user-100", "user-101", "user-102"]
        },
        notificationTypes: ["SMS"],
        senderInfos: {
            SMS: {
                senderPhoneNumber: "02-1234-5678",
                senderName: "Sales Team"
            }
        },
        content: {
            title: "[Special Sale]",
            body: "VIP 고객님을 위한 특별 할인! 앱에서 확인하세요. (~7/31)",
            redirectUrl: "https://sale.example.com",
            imageUrl: null
        },
        scheduledAt: new Date(Date.now() + 60 * 1000).toISOString(), // 1분 후 예약 발송
        memo: "VIP 고객 할인 안내 SMS 예약발송"
    },

    "단일 회원, 템플릿 기반 푸시 즉시 발송": {
        requester: { type: "USER", id: "user-sender-789" },
        recipients: {
            userIds: ["user-555"]
        },
        notificationTypes: ["PUSH"],
        senderInfos: {
            PUSH: { senderName: "Shopping App" }
        },
        template: {
            templateId: "ORDER_SHIPPED",
            templateParameters: {
                orderId: "ORD-20250618-001",
                productName: "Wireless Earbuds",
                shippingCompany: "FastCourier",
                trackingNumber: "1234567890"
            }
        },
        scheduledAt: null,
        memo: "주문 발송 푸시 알림"
    },

    "전체 회원 대상, 직접 작성 푸시 공지 즉시 발송": {
        requester: { type: "ADMIN", id: "admin-system" },
        recipients: { allUsers: true },
        notificationTypes: ["PUSH"],
        senderInfos: {
            PUSH: { senderName: "System Notice" }
        },
        content: {
            title: "🚨 시스템 점검 안내",
            body: "2025년 7월 1일 02:00~05:00 시스템 점검이 예정되어 있습니다.",
            redirectUrl: "https://myservice.com/notice",
            imageUrl: null
        },
        scheduledAt: null,
        memo: "전체 회원 대상 시스템 점검 공지"
    },

    "특정 세그먼트 회원 대상 템플릿 이메일 예약 발송": {
        requester: { type: "SERVICE", id: "marketing-automation" },
        recipients: {
            segment: "LOYAL_CUSTOMERS_PURCHASE_OVER_1M"
        },
        notificationTypes: ["EMAIL"],
        senderInfos: {
            EMAIL: {
                senderEmailAddress: "promo@myservice.com",
                senderName: "Promotion Team"
            }
        },
        template: {
            templateId: "VIP_DISCOUNT_EMAIL",
            templateParameters: {
                discountRate: "15%",
                couponCode: "VIP2025SUMMER"
            }
        },
        scheduledAt: new Date(Date.now() + 5 * 60 * 1000).toISOString(), // 5분 후 예약 발송
        memo: "충성고객 대상 프로모션 이메일"
    }
};

(async () => {
    for (const [description, payload] of Object.entries(testCases)) {
        try {
            const res = await axios.post(BASE_URL, payload, {
                headers: {
                    'Content-Type': 'application/json',
                    'Idempotency-Key': randomUUID().toString() // Node.js 내장 함수 사용
                }
            });

            console.log(`[✅ SUCCESS] ${description}: ${res.status}`);
        } catch (err) {
            console.error(`[❌ FAIL] ${description}: [${err.response?.status}] ${err.message}`);
            console.error(err.response?.data);
        }
    }
})();